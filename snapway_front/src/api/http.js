import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL;
const http = axios.create({
  baseURL: baseURL + "/api",
  withCredentials: true,
  timeout: 5000,
});

// 쿠키에서 XSRF-TOKEN 읽는 함수
const getCookie = (name) => {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";")[0];
  return null;
};

// 매 요청 전에 CSRF 헤더 추가
http.interceptors.request.use((config) => {
  const method = (config.method || "get").toUpperCase();

  // GET은 굳이 안 붙여도 되지만, 보통 state 변경 메서드에만 붙임
  if (["POST", "PUT", "DELETE", "PATCH"].includes(method)) {
    const token = getCookie("XSRF-TOKEN"); // CookieCsrfTokenRepository 기본 이름
    if (!!token) {
      config.headers["X-XSRF-TOKEN"] = token;
    }
  }
  return config;
});

// TODO: JWT 사용 시 여기서 Authorization 헤더 인터셉터 설정
// http.interceptors.request.use(config => { ... })

export default http;
