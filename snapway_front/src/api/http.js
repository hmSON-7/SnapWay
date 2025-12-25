import axios from "axios";
import { reissueToken } from "@/api/authApi";

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

let isRefreshing = false; // 현재 토큰 갱신 중인지 여부 (Lock)
let failedQueue = [];     // 갱신 동안 대기 중인 요청들

const processQueue = (error, token = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

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
  async (error) => {
    const originalRequest = error.config;

    // A. 401 에러가 발생했고, 아직 재시도 안 한 요청인 경우
    if (error.response && error.response.status === 401 && !originalRequest._retry) {
      
      // [예외] 로그인, 로그아웃, 재발급 요청 자체가 실패한 경우는 그냥 에러 처리
      if (
          originalRequest.url.includes('/login') || 
          originalRequest.url.includes('/reissue') ||
          originalRequest.url.includes('/logout')
      ) {
          return Promise.reject(error);
      }

      // B. 이미 다른 요청이 갱신 중이라면 -> 큐에 넣고 대기
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
        .then((newToken) => {
          originalRequest.headers['Authorization'] = `Bearer ${newToken}`;
          return http(originalRequest);
        })
        .catch((err) => Promise.reject(err));
      }

      // C. 갱신 시작
      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const accessToken = localStorage.getItem("accessToken");
        const refreshToken = localStorage.getItem("refreshToken"); // 스토리지에 있는지 확인

        if (!refreshToken) {
            throw new Error("리프레시 토큰 없음");
        }

        // D. 재발급 API 호출
        // authApi.js에서 만든 reissueToken은 순수 axios를 쓰므로 인터셉터 안 탐
        const { data } = await reissueToken(accessToken, refreshToken);
        
        // E. 응답값 (accessToken, refreshToken)
        const { accessToken: newAccess, refreshToken: newRefresh } = data;

        // F. 로컬 스토리지 갱신
        localStorage.setItem("accessToken", newAccess);
        localStorage.setItem("refreshToken", newRefresh);
        
        // G. 헤더 교체
        http.defaults.headers.common["Authorization"] = `Bearer ${newAccess}`;
        originalRequest.headers["Authorization"] = `Bearer ${newAccess}`;

        // H. 대기열 해소 (Resolve)
        processQueue(null, newAccess);

        // I. 실패했던 요청 재시도
        return http(originalRequest);

      } catch (err) {
        // J. 재발급 실패 (리프레시 토큰 만료 등) -> 로그아웃 처리
        processQueue(err, null);
        
        // 로그아웃 (스토리지 비우기)
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");
        localStorage.removeItem("loginUser");

        alert("세션이 만료되었습니다. 다시 로그인해주세요.");
        window.location.href = "/"; // 홈으로 이동
        
        return Promise.reject(err);
      } finally {
        isRefreshing = false;
      }
    }

    // 그 외 에러는 그냥 반환
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
