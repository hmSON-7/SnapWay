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

// await initCsrf();
app.use(router).use(pinia).mount("#app");
