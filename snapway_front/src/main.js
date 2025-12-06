import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import "@fortawesome/fontawesome-free/css/all.min.css";
import api from "@/api/http.js";

const app = createApp(App);
const pinia = createPinia();

const initCsrf = async () => {
  await api.post("/csrf"); // 쿠키(XSRF-TOKEN) + 응답 JSON(CsrfToken) 생성
};

await initCsrf();
app.use(router).use(pinia).mount("#app");
