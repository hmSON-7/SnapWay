package com.snapway.model.dto;

import lombok.Data;

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
}