// src/main.js
import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import "@fortawesome/fontawesome-free/css/all.min.css";
import http, { csrfClient } from "@/api/http.js";

const app = createApp(App);
const pinia = createPinia();

// csrf 사용시 활성화
// const initCsrf = async () => {
//   // 실제 요청: POST http://localhost:8080/api/csrf
//   await csrfClient.post("/api/csrf");
// };

const loadKakaoMap = () => {
    const apiKey = import.meta.env.VITE_KEY_KAKAO_JAVASCRIPT;
    
    if (!apiKey) {
        console.error("Kakao Map API Key가 설정되지 않았습니다.");
        return;
    }

    const script = document.createElement("script");
    // libraries 파라미터 필수 (services: 장소검색, clusterer: 마커클러스터)
    script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false&libraries=services,clusterer,drawing`;
    script.async = true;
    
    // 스크립트 로드 완료 후 카카오맵 로드
    script.onload = () => {
        window.kakao.maps.load(() => {
        console.log("Kakao Map Loaded");
        });
    };

    document.head.appendChild(script);
};

// await initCsrf();
app.use(router).use(pinia).mount("#app");
