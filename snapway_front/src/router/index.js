import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue";
import Login from "../views/login.vue"; // 🔹 새로 추가

const routes = [
  { path: "/", name: "home", component: Home },
  { path: "/login", name: "login", component: Login }, // 🔹 로그인 라우트
  // 이후 /mypage 등도 여기에서 추가
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
