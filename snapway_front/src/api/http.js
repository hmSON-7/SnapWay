// src/api/http.js
import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";

// 일반 API 호출용
const http = axios.create({
  baseURL: baseURL + "/api",
  withCredentials: true,
  timeout: 5000,
});

// CSRF 전용 클라이언트 (루트 기준)
export const csrfClient = axios.create({
  baseURL, // http://localhost:8081
  withCredentials: true,
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 쿠키에서 XSRF-TOKEN 읽기
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";")[0];
  return null;
}

// 상태 변경 요청에 자동으로 CSRF 헤더 추가
http.interceptors.request.use((config) => {
  const method = (config.method || "get").toUpperCase();
  if (["POST", "PUT", "DELETE", "PATCH"].includes(method)) {
    const token = getCookie("XSRF-TOKEN");
    if (token) {
      config.headers["X-XSRF-TOKEN"] = token; // 🔴 쿠키값 그대로
    }
  }
  return config;
});

export default http;
