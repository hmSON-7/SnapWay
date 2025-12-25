import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL;

// 1. 일반 API 호출용 Axios 인스턴스 생성
const http = axios.create({
  baseURL: baseURL + "/api",
  headers: {
    "Content-Type": "application/json;charset=utf-8",
  },
  withCredentials: true, // CORS 상황에서 쿠키(XSRF-TOKEN)를 주고받기 위해 필요할 수 있음
});

// 쿠키에서 값을 읽어오는 헬퍼 함수
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(";")[0];
  return null;
}

// 2. 요청 인터셉터 (Request Interceptor) - 하나로 통합됨
http.interceptors.request.use(
  (config) => {
    // 헤더 객체가 없으면 빈 객체로 초기화 (방어 코드)
    config.headers = config.headers || {};

    // -----------------------------------------------------------
    // [NEW] FormData 처리 - Content-Type 자동 설정
    // -----------------------------------------------------------
    if (config.data instanceof FormData) {
      // FormData일 때는 브라우저가 자동으로 multipart/form-data 설정
      delete config.headers["Content-Type"];
    }

    // -----------------------------------------------------------
    // [Logic A] JWT 토큰 처리 (Authorization)
    // -----------------------------------------------------------
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }

    // -----------------------------------------------------------
    // [Logic B] CSRF 토큰 처리 (X-XSRF-TOKEN)
    // -----------------------------------------------------------
    // GET 요청 등을 제외한 상태 변경 요청(POST, PUT, DELETE 등)에만 적용
    const method = (config.method || "get").toUpperCase();
    const isStateChangeMethod = ["POST", "PUT", "DELETE", "PATCH"].includes(
      method
    );

    if (isStateChangeMethod) {
      const xsrfToken = getCookie("XSRF-TOKEN");
      if (xsrfToken) {
        // Spring Security 기본 헤더명은 'X-XSRF-TOKEN'
        config.headers["X-XSRF-TOKEN"] = xsrfToken;
      }
    }

    return config;
  },
  (error) => {
    console.error("Axios Request Error:", error);
    return Promise.reject(error);
  }
);

// 3. 응답 인터셉터 (Response Interceptor)
http.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      console.warn("인증 실패: 토큰이 만료되었거나 유효하지 않습니다.");
      // 필요 시 로그아웃 로직 추가
      // localStorage.removeItem("accessToken");
      // window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

// 4. CSRF 토큰 발급용 등 특수 목적 클라이언트 (필요 시 사용)
export const csrfClient = axios.create({
  baseURL, // http://localhost:8081 (루트 경로)
  withCredentials: true,
  timeout: 5000,
  headers: {
    "Content-Type": "application/json",
  },
});

export default http;
