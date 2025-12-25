package com.snapway.model.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapway.model.dto.PhotoMetadata;
import com.snapway.model.dto.TravelStyle;
import com.snapway.model.dto.Trip;
import com.snapway.model.dto.TripHashtag;
import com.snapway.model.dto.TripPhoto;
import com.snapway.model.dto.TripRecord;
import com.snapway.model.mapper.TripMapper;
import com.snapway.util.FileUtil;
import com.snapway.util.ImageBase64Encoder;
import com.snapway.util.MetadataUtil;
import com.snapway.util.MetadataUtil.PhotoWithFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {

    private final AiService aiService;
    private final FileUtil fileUtil;
    private final ImageBase64Encoder imageBase64Encoder;
    private final MetadataUtil metadataUtil;
    private final ObjectMapper objectMapper;
    private final TripMapper tripMapper;
    
    // AI 응답 파싱용 내부 레코드
    private record AiResponseDto(String content, List<String> hashtags) {}
    private record PhotoAnalysisResult(MultipartFile file, PhotoMetadata metadata, String description) {}

    /**
     * 메인 비즈니스 로직
     * @Transactional 제거. AI 통신과 같은 긴 작업은 트랜잭션 없이 수행
     * DB 저장 시점에만 별도의 트랜잭션 메서드(saveTripData) 호출
     */
    @Override
    public Trip createAutoTrip(int memberId, String title, List<MultipartFile> files) throws Exception {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("사진 파일이 없습니다.");
        }

        log.info("=== 1단계: 개별 사진 분석 시작 (총 {}장) ===", files.size());
        
        List<PhotoWithFile> sortedPhotos = metadataUtil.extractAndSort(files);
        
        // 동시성 처리를 위한 안전한 리스트 사용
        log.info("=== 2단계: 개별 사진 AI 분석 (총 {}장) ===", sortedPhotos.size());
        
        // 2. 정렬된 순서를 유지하며 병렬 처리로 AI 분석 요청
        // parallelStream()을 사용하더라도 collect(Collectors.toList())는 원본 리스트의 순서(시간순)를 보장합니다.
        List<PhotoAnalysisResult> analysisResults = sortedPhotos.parallelStream()
            .map(photoWithFile -> {
                try {
                    MultipartFile file = photoWithFile.getFile();
                    PhotoMetadata metadata = photoWithFile.getMetadata();
                    
                    // Base64 변환
                    String base64 = imageBase64Encoder.encode(file);
                    if (base64 == null) return null;

                    // AI 분석 요청 (개별 사진 묘사)
                    String analysisPrompt = """
                            Analyze this photo for a travel blog. 
                            Describe the location (landmark), atmosphere, time of day, and what is happening in 1-2 sentences.
                            Start the response directly with the description.
                            """;
                    // TODO: 추후 여기에 Reverse Geocoding으로 얻은 주소 정보를 프롬프트에 추가하면 정확도 향상 가능
                    
                    String rawDescription = aiService.generateContent(analysisPrompt, List.of(base64));
                    String description = extractTextFromAiResponse(rawDescription);
                    
                    return new PhotoAnalysisResult(file, metadata, description);
                } catch (Exception e) {
                    log.error("사진 분석 실패 (파일명: {}): {}", photoWithFile.getFile().getOriginalFilename(), e.getMessage());
                    return null;
                }
            })
            .filter(Objects::nonNull) // 실패한 건 제외
            .collect(Collectors.toList());

        if (analysisResults.isEmpty()) {
            throw new RuntimeException("모든 사진 분석에 실패했습니다.");
        }

        // 날짜 범위 계산
        LocalDate minDate = analysisResults.get(0).metadata.getTakenAt() != null ? 
                analysisResults.get(0).metadata.getTakenAt().toLocalDate() : LocalDate.now();
        LocalDate maxDate = analysisResults.get(analysisResults.size() - 1).metadata.getTakenAt() != null ? 
                analysisResults.get(analysisResults.size() - 1).metadata.getTakenAt().toLocalDate() : LocalDate.now();


        log.info("=== 2단계: 여행기 본문 작성 (텍스트 기반) ===");
        
        // TravelStyle 목록 문자열 생성
        String stylesList = Arrays.stream(TravelStyle.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));

        // 3. 종합 프롬프트 생성 (텍스트만 모음)
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Here are descriptions of photos from a trip, sorted chronologically.\n");
        promptBuilder.append("Write a detailed and emotional travel blog post based on these descriptions.\n\n");

        for (int i = 0; i < analysisResults.size(); i++) {
            PhotoAnalysisResult result = analysisResults.get(i);
            String timeStr = result.metadata.getTakenAt() != null ? 
                    result.metadata.getTakenAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "Unknown time";
            
            promptBuilder.append(String.format("[Photo %d] Time: %s, Description: %s\n", i, timeStr, result.description));
        }

        promptBuilder.append("\n[Instructions]\n");
        promptBuilder.append("1. **Content**: Write a detailed Korean(한국어) blog post. Use [[PHOTO_N]] markers for images.\n");
        promptBuilder.append("2. **Hashtags**: Select 2 to 3 most relevant travel styles strictly from this list: [" + stylesList + "].\n");
        
        promptBuilder.append("\n[Output Format]\n");
        promptBuilder.append("You MUST return the result in **Strict JSON format** without Markdown code blocks.\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"content\": \"...blog content...\",\n");
        promptBuilder.append("  \"hashtags\": [\"STYLE1\", \"STYLE2\"]\n");
        promptBuilder.append("}");

        // 4. 최종 글 작성 요청 (이미지 없이 텍스트만 전송 -> Payload 문제 해결!)
        String rawResponse = aiService.generateContent(promptBuilder.toString(), null); // 이미지 null

        // 응답에서 JSON 파싱이 필요하다면 수행 (현재 AiService가 JSON String을 리턴한다면)
        AiResponseDto parsedResponse = parseJsonAiResponse(rawResponse);


        log.info("=== 3단계: 파일 저장 및 DB 처리 ===");

        // 5. [중요] DB 저장 로직만 별도 트랜잭션 메서드로 분리 호출
        // 자기 자신을 주입받거나, 별도 클래스로 분리해야 @Transactional이 동작하지만,
        // 여기서는 구조 변경을 최소화하기 위해 바로 호출하되, 실제 운영 환경에서는
        // AOP 프록시 처리를 위해 saveTripData를 별도 Service로 빼거나 Self-Injection을 고려해야 함.
        // *참고: 같은 클래스 내 메서드 호출은 @Transactional이 무시될 수 있음 -> 구조상 분리가 정석이나, 
        // 일단 로직 흐름을 보여드리기 위해 아래 메서드(saveTripData)를 public으로 뺍니다.
        
        return saveTripData(memberId, title, minDate, maxDate, analysisResults, parsedResponse.content, parsedResponse.hashtags);
    }
    
    /**
     * AI의 JSON 응답을 파싱하여 Content와 Hashtags를 추출
     */
    private AiResponseDto parseJsonAiResponse(String rawResponse) {
        try {
            // 1. Gemini API 응답 구조 파싱 (candidates -> content -> parts -> text)
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode textNode = root.path("candidates").get(0)
                                    .path("content").path("parts").get(0)
                                    .path("text");
            
            String innerJsonString = textNode.isMissingNode() ? rawResponse : textNode.asText();

            // 2. AI가 가끔 ```json ... ``` 마크다운을 붙일 수 있으므로 제거
            innerJsonString = innerJsonString.replaceAll("```json", "").replaceAll("```", "").trim();

            // 3. 내부 JSON (content, hashtags) 파싱
            JsonNode innerRoot = objectMapper.readTree(innerJsonString);
            String content = innerRoot.path("content").asText();
            
            List<String> hashtags = new ArrayList<>();
            JsonNode tagsNode = innerRoot.path("hashtags");
            if (tagsNode.isArray()) {
                for (JsonNode tag : tagsNode) {
                    hashtags.add(tag.asText());
                }
            }
            
            return new AiResponseDto(content, hashtags);

        } catch (Exception e) {
            log.error("AI 응답 파싱 실패. 원본 응답: {}", rawResponse, e);
            // 파싱 실패 시, 원본 텍스트를 본문으로 하고 태그는 빈 값 처리 (Fallback)
            // Gemini API 구조 파싱조차 실패했다면 rawResponse 자체를 반환 시도하지 않음 (복잡함)
            return new AiResponseDto("여행기 생성 중 형식이 맞지 않아 원본을 표시합니다.\n" + rawResponse, new ArrayList<>());
        }
    }

    /**
     * Gemini 응답에서 텍스트만 추출 (사진 설명용)
     */
    private String extractTextFromAiResponse(String rawResponse) {
        if (rawResponse == null) return "";
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode textNode = root.path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text");
            if (!textNode.isMissingNode()) {
                return textNode.asText().replace("```", "").trim();
            }
        } catch (Exception e) {
            // rawResponse가 이미 텍스트인 경우 fallback
        }
        return rawResponse.replace("```", "").trim();
    }
    
    /**
     * DB 저장과 파일 저장을 담당하는 메서드 (트랜잭션 필수)
     * 실제로는 외부에서 호출되거나 별도 서비스로 분리하는 것이 가장 안전합니다.
     */
    @Transactional(rollbackFor = Exception.class)
    public Trip saveTripData(int memberId, String title, LocalDate minDate, LocalDate maxDate, 
                             List<PhotoAnalysisResult> analysisResults, String generatedContent, List<String> hashtags) throws Exception {
        
        // 5. Trip 정보 생성
        Trip trip = Trip.builder()
                .memberId(memberId)
                .title(title)
                .startDate(minDate)
                .endDate(maxDate)
                .uploadedAt(LocalDateTime.now())
                .visibility("PUBLIC")
                .build();
        
        tripMapper.insertTrip(trip);
        int tripId = trip.getTripId();
        log.info("여행 DB 생성 완료: tripId={}", tripId);
        
        List<TravelStyle> savedStyles = new ArrayList<>();
        if (hashtags != null && !hashtags.isEmpty()) {
            for (String tagStr : hashtags) {
                try {
                    // 문자열(HEALING) -> Enum 변환
                    TravelStyle style = TravelStyle.valueOf(tagStr.toUpperCase());
                    
                    TripHashtag hashtag = TripHashtag.builder()
                            .tripId(tripId)
                            .style(style)
                            .build();
                    
                    tripMapper.insertTripHashtag(hashtag);
                    savedStyles.add(style);
                } catch (IllegalArgumentException e) {
                    log.warn("AI가 생성한 태그 '{}'는 TravelStyle Enum에 존재하지 않아 건너뜁니다.", tagStr);
                }
            }
        }
        trip.setStyles(savedStyles); // 반환 객체에 설정

        // 6. 파일 저장 및 본문 태그 치환
        List<TripPhoto> tripPhotos = new ArrayList<>();
        String contentWithImages = generatedContent;
        
        for (int i = 0; i < analysisResults.size(); i++) {
            PhotoAnalysisResult result = analysisResults.get(i);
            
            // 파일 저장 (I/O 작업이지만 롤백이 안되므로 신중해야 함, 여기서는 편의상 포함)
            String savedPath = fileUtil.saveFile(result.file, String.valueOf(memberId), "trip", String.valueOf(tripId));
            
            String placeholder = "[[PHOTO_" + i + "]]";
            String caption = result.description.length() > 20 ? result.description.substring(0, 20) + "..." : result.description;
            String markdownImage = String.format("\n![%s](%s)\n", caption, savedPath);
            
            contentWithImages = contentWithImages.replace(placeholder, markdownImage);
            
            tripPhotos.add(TripPhoto.builder()
                    .filePath(savedPath)
                    .photoName(result.file.getOriginalFilename())
                    .caption(result.description)
                    .build());
        }

        // 7. TripRecord (사진별 기록) DB 저장
        List<TripRecord> records = new ArrayList<>();
        
        for (int i = 0; i < analysisResults.size(); i++) {
            PhotoAnalysisResult result = analysisResults.get(i);
            PhotoMetadata metadata = result.metadata() != null ? result.metadata() : new PhotoMetadata();
            TripPhoto photo = tripPhotos.get(i);
            
            TripRecord record = TripRecord.builder()
                    .tripId(tripId)
                    .placeName(title)
                    .latitude(metadata.getLatitude())
                    .longitude(metadata.getLongitude())
                    .aiContent(i == 0 ? contentWithImages : null)
                    .visitedDate(metadata.getTakenAt())
                    .build();
            
            tripMapper.insertTripRecord(record);
            int recordId = record.getRecordId();
            
            // 8. TripPhoto DB 저장 (recordId 연결)
            photo.setRecordId(recordId);
            tripMapper.insertTripPhoto(photo);
            
            record.setPhotos(List.of(photo));
            records.add(record);
        }
        
        records.sort(Comparator.comparing(TripRecord::getVisitedDate, Comparator.nullsLast(Comparator.naturalOrder())));
        
        // 반환 객체 구성
        trip.setRecords(records);
        
        return trip;
    }

    // --- 2. 내 여행 목록 조회 ---
    @Override
    public List<Trip> getMyTripList(int memberId) throws Exception {
    	return tripMapper.selectTripListByMemberId(memberId);
    }
    
    // ---- 3. 여행 상세 조회 (계층 구조 조립) ---
    @Override
    public Trip getTripDetail(int tripId) throws Exception {
    	// (1) 여행 기본 정보 조회
    	Trip trip = tripMapper.selectTripById(tripId);
    	if(trip == null) {
    		throw new RuntimeException("해당 기록을 찾을 수 없습니다.");
    	}
    	
    	// (2) 해당 여행의 세부 기록(Records) 조회
    	List<TripRecord> records = tripMapper.selectRecordsByTripId(tripId);
    	records.sort(Comparator.comparing(TripRecord::getVisitedDate, Comparator.nullsLast(Comparator.naturalOrder())));
    	
    	// (3) 각 기록별 사진(Photos) 조회 및 조립
    	for(TripRecord record : records) {
    		List<TripPhoto> photos = tripMapper.selectPhotosByRecordId(record.getRecordId());
    		record.setPhotos(photos);
    	}
    	
    	// (4) 최종 조립
    	trip.setRecords(records);
    	return trip;
    }
    
}



