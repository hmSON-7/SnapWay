import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import "@fortawesome/fontawesome-free/css/all.min.css";
import http, { csrfClient } from "@/api/http.js";

const app = createApp(App);
const pinia = createPinia();

// csrf 사용예시
const initCsrf = async () => {
  await csrfClient.post("/api/csrf");
};

// Kakao 지도 SDK 로드가 끝나야 window.kakao가 준비됨
const loadKakaoMap = () =>
  new Promise((resolve, reject) => {
    const apiKey = import.meta.env.VITE_KEY_KAKAO_JAVASCRIPT || "61d6f7caf18ddf367b0771ad86ee8b16";

    if (!apiKey) {
      console.error("Kakao Map API Key가 설정되지 않았습니다.");
      reject(new Error("Kakao Map API Key missing"));
      return;
    }

    // 이미 로드된 경우 바로 완료
    if (window.kakao && window.kakao.maps) {
      resolve();
      return;
    }

    const script = document.createElement("script");
    const src =
      "https://dapi.kakao.com/v2/maps/sdk.js?appkey=" +
      apiKey +
      "&autoload=false&libraries=services,clusterer,drawing";
    script.src = src;
    script.async = true;
    script.onload = () => {
      window.kakao.maps.load(() => {
        console.log("Kakao Map Loaded");
        resolve();
      });
    };
    script.onerror = (err) => {
      console.error("Kakao Map script 로딩 실패", err);
      reject(err);
    };
    document.head.appendChild(script);
  });

// await initCsrf();
loadKakaoMap()
  .catch(() => {
    // 지도 스크립트가 없어도 앱은 마운트
  })
  .finally(() => {
    app.use(router).use(pinia).mount("#app");
  });
