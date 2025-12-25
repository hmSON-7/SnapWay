package com.snapway.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    public static class EmailRequest {
        private String email;
    }

    @Data
    public static class VerifyCodeRequest {
        private String email;
        private String code;
    }
    
    @Data
    public static class VerifyResponse {
        private String resetToken; // 인증 성공 시 발급되는 임시 토큰
    }

    @Data
    public static class PasswordResetRequest {
        private String email;
        private String newPassword;
        private String resetToken; // 인증 성공 증명 토큰
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TokenResponse {
    	private String accessToken;
    	private String refreshToken;
    }
    
    @Data
    public static class ReissueRequest {
    	private String accessToken;
    	private String refreshToken;
    }
}