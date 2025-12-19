package com.snapway.model.service;

public interface AiService {
	
    /**
     * AI 모델에 프롬프트를 전송하고 응답을 반환합니다.
     * @param prompt 사용자 입력 텍스트
     * @return AI 응답 텍스트
     */
    String generateContent(String prompt);
}