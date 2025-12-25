// src/api/authApi.js
import http from "./http";
import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL;

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

// 4. 토큰 재발급 요청 (순수 axios 사용)
export const reissueToken = async (accessToken, refreshToken) => {
    // http 인스턴스가 아닌 axios를 직접 사용해야 인터셉터 루프에 빠지지 않음
    return axios.post(
        `${baseURL}/api/auth/reissue`, 
        { accessToken, refreshToken },
        { 
            headers: { "Content-Type": "application/json" },
            withCredentials: true,
        }
    );
};