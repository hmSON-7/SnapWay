// src/api/authApi.js
import http from "./http";

// 1. 인증 번호 발송 요청
export const sendVerificationCode = (email) => {
    return http.post("/auth/password/code-request", { email });
};

// 2. 인증 번호 검증 (성공 시 resetToken 반환됨)
export const verifyCode = (email, code) => {
    return http.post("/auth/password/code-verify", { email, code });
};

// 3. 비밀번호 재설정
export const resetPassword = (email, newPassword, resetToken) => {
    return http.post("/auth/password/reset", {
        email,
        newPassword,
        resetToken,
    });
};