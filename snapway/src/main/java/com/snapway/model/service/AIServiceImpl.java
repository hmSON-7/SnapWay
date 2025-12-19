package com.snapway.model.service;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
		Map<String, Object> requestBody = Map.of(
			"contents", List.of(
				Map.of(
					"parts", List.of(
						Map.of("text", prompt)
					)
				)
			)
		);
		
		log.info("Sending request to GMS: {}", gmsApiUrl);
		
		URI uri = UriComponentsBuilder.fromUriString(gmsApiUrl)
                .queryParam("key", gmsApiKey) // API 키 추가
                .build()
                .toUri();
		
		String response = webClientBuilder.build()
				.post()
				.uri(uri) 
	            .contentType(MediaType.APPLICATION_JSON)
	            .bodyValue(requestBody)
	            .retrieve()
	            .bodyToMono(String.class) // 우선 전체 JSON 문자열로 받음
	            .block(); // 동기 방식 호출 (필요시 subscribe로 비동기 변경)
		
		return response;
	}

}
