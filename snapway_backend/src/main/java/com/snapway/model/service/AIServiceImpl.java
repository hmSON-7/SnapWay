package com.snapway.model.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl implements AiService {
	
	private final WebClient.Builder webClientBuilder;
	
	@Value("${gms.api.url}")
	private String gmsApiUrl;
	
	@Value("${gms.api.key}")
	private String gmsApiKey;
	
	@Override
	public String generateContent(String prompt) {
		return generateContent(prompt, null);
	}
	
	@Override
	public String generateContent(String prompt, List<String> base64Images) {
		// 1. 요청 본문 구성
		List<Map<String, Object>> parts = new ArrayList<>();
		
		if (base64Images != null && !base64Images.isEmpty()) {
            for (String base64Image : base64Images) {
                // Base64 문자열 유효성 체크 (빈 값 방지)
                if (base64Image == null || base64Image.isEmpty()) continue;

                parts.add(Map.of(
                    "inlineData", Map.of(
                        "mimeType", "image/jpeg", 
                        "data", base64Image
                    )
                ));
            }
        }
		
		parts.add(Map.of("text", prompt));
		
		Map<String, Object> content = Map.of(
            "role", "user",
            "parts", parts
        );
		
		// 2. 전체 요청 구조 생성
		// { "contents": [{ "parts": [ {text...}, {inlineData...}, ... ] }] }
		Map<String, Object> requestBody = Map.of(
			"contents", List.of(content)
		);
        
        // 3. URI 생성
		URI uri = UriComponentsBuilder.fromUriString(gmsApiUrl)
                .queryParam("key", gmsApiKey) // API 키 추가
                .build()
                .toUri();
		
		// 4. API 호출
		try {
            String response = webClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // 동기 호출

            return response;
        } catch (WebClientResponseException e) {
            // [중요] 400, 500 에러 시 서버가 보낸 상세 응답을 로그에 출력
            log.error("GMS API 호출 실패 (Status: {}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("AI API 호출 오류: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("AI 서비스 내부 오류: {}", e.getMessage());
            throw new RuntimeException("AI 서비스 연결 실패", e);
        }
	}

}
