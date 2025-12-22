package com.snapway.model.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapway.model.dto.PhotoMetadata;
import com.snapway.model.dto.Trip;
import com.snapway.model.dto.TripPhoto;
import com.snapway.model.dto.TripRecord;
import com.snapway.model.mapper.TripMapper;
import com.snapway.util.FileUtil;
import com.snapway.util.ImageBase64Encoder;
import com.snapway.util.MetadataUtil;

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

    // private final TripMapper tripMapper; // MyBatis 매퍼

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Trip createAutoTrip(int memberId, String title, List<MultipartFile> files) throws Exception {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("사진 파일이 없습니다.");
        }

        log.info("=== 1단계: 개별 사진 분석 시작 (총 {}장) ===", files.size());
        
        // 동시성 처리를 위한 안전한 리스트 사용
        List<PhotoAnalysisResult> analysisResults = new CopyOnWriteArrayList<>();

        // 1. [병렬 처리] 각 사진을 하나씩 AI에게 보내서 묘사(Description)를 받아옴
        // parallelStream을 사용하여 여러 사진을 동시에 분석 (속도 향상)
        files.parallelStream().forEach(file -> {
            try {
                // 1-1. 메타데이터 및 Base64 준비
                PhotoMetadata metadata = metadataUtil.extractMetadata(file);
                String base64 = imageBase64Encoder.encode(file);
                
                if (base64 == null) return; // 변환 실패 시 건너뜀

                // 1-2. 개별 사진 분석 요청 (이미지 1장이라 가벼움)
                String analysisPrompt = """
                        Analyze this photo for a travel blog. 
                        Describe the location (landmark), atmosphere, time of day, and what is happening in 1-2 sentences.
                        Start the response directly with the description.
                        """;
                
                // 이미지를 1장만 포함하여 전송
                String description = aiService.generateContent(analysisPrompt, List.of(base64));
                
                // 1-3. 결과 저장
                // 원본 리스트의 인덱스를 추적하기 위해 indexOf 등을 쓰지 않고, 별도 로직이 필요하나
                // 여기서는 파일 객체 자체를 키로 쓰거나 분석 결과에 포함시킴
                analysisResults.add(new PhotoAnalysisResult(file, metadata, description));
                
            } catch (Exception e) {
                log.error("사진 분석 실패 (파일명: {}): {}", file.getOriginalFilename(), e.getMessage());
                // 실패해도 다른 사진들은 계속 진행
            }
        });

        if (analysisResults.isEmpty()) {
            throw new RuntimeException("모든 사진 분석에 실패했습니다.");
        }

        // 2. 시간 순서대로 정렬 (여행기는 시간 순이므로)
        analysisResults.sort(Comparator.comparing(
            r -> r.metadata.getTakenAt() != null ? r.metadata.getTakenAt() : LocalDateTime.now()
        ));

        // 날짜 범위 계산
        LocalDate minDate = analysisResults.get(0).metadata.getTakenAt() != null ? 
                analysisResults.get(0).metadata.getTakenAt().toLocalDate() : LocalDate.now();
        LocalDate maxDate = analysisResults.get(analysisResults.size() - 1).metadata.getTakenAt() != null ? 
                analysisResults.get(analysisResults.size() - 1).metadata.getTakenAt().toLocalDate() : LocalDate.now();


        log.info("=== 2단계: 여행기 본문 작성 (텍스트 기반) ===");

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

        promptBuilder.append("\n[Writing Rules]\n");
        promptBuilder.append("1. Write in Korean (한국어).\n");
        promptBuilder.append("2. Insert the tag [[PHOTO_N]] (e.g., [[PHOTO_0]]) naturally where each photo should be displayed.\n");
        promptBuilder.append("3. Do not include a title line, just the blog content.\n");
        promptBuilder.append("4. Make it sound like a personal diary.\n");

        // 4. 최종 글 작성 요청 (이미지 없이 텍스트만 전송 -> Payload 문제 해결!)
        String fullStory = aiService.generateContent(promptBuilder.toString(), null); // 이미지 null

        // 응답에서 JSON 파싱이 필요하다면 수행 (현재 AiService가 JSON String을 리턴한다면)
        String generatedContent = parseAiResponse(fullStory);


        log.info("=== 3단계: 파일 저장 및 DB 처리 ===");

        // 5. Trip 정보 생성
        Trip trip = Trip.builder()
                .memberId(memberId)
                .title(title)
                .startDate(minDate)
                .endDate(maxDate)
                .uploadedAt(LocalDateTime.now())
                .visibility("PUBLIC")
                .build();
        
        // tripMapper.insertTrip(trip);
        int tripId = 1; // 임시 ID

        // 6. 파일 저장 및 본문 태그 치환
        List<TripPhoto> tripPhotos = new ArrayList<>();
        
        for (int i = 0; i < analysisResults.size(); i++) {
            PhotoAnalysisResult result = analysisResults.get(i);
            
            // 파일 저장
            String savedPath = fileUtil.saveFile(result.file, String.valueOf(memberId), "trip", String.valueOf(tripId));
            
            // 태그 치환: [[PHOTO_0]] -> ![설명](/img/...)
            String placeholder = "[[PHOTO_" + i + "]]";
            // 캡션(설명)으로 AI가 분석한 내용을 간단히 넣어줄 수 있음 (20자 제한 등)
            String caption = result.description.length() > 20 ? result.description.substring(0, 20) + "..." : result.description;
            String markdownImage = String.format("\n![%s](%s)\n", caption, savedPath);
            
            generatedContent = generatedContent.replace(placeholder, markdownImage);
            
            tripPhotos.add(TripPhoto.builder()
                    .filePath(savedPath)
                    .photoName(result.file.getOriginalFilename())
                    .caption(result.description) // AI 분석 결과를 캡션으로 활용
                    .build());
        }

        TripRecord record = TripRecord.builder()
                .tripId(tripId)
                .placeName(title)
                .aiContent(generatedContent)
                .visitedDate(analysisResults.get(0).metadata.getTakenAt())
                .photos(tripPhotos)
                .build();
        
        trip.setRecords(List.of(record));
        
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
    	
    	// (3) 각 기록별 사진(Photos) 조회 및 조립
    	for(TripRecord record : records) {
    		List<TripPhoto> photos = tripMapper.selectPhotosByRecordId(record.getRecordId());
    		record.setPhotos(photos);
    	}
    	
    	// (4) 최종 조립
    	trip.setRecords(records);
    	return trip;
    }
    
    private String parseAiResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode textNode = root.path("candidates").get(0)
                                    .path("content").path("parts").get(0)
                                    .path("text");
            return textNode.isMissingNode() ? jsonResponse : textNode.asText();
        } catch (Exception e) {
            // 파싱 실패 시 원본 반환 (혹은 에러 처리)
            return jsonResponse;
        }
    }

    // 내부 데이터 클래스
    private record PhotoAnalysisResult(MultipartFile file, PhotoMetadata metadata, String description) {}

}