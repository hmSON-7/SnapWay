import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue";
import Regist from "../views/Regist.vue"; // 🔹 새로 추가

const routes = [
  { path: "/", name: "home", component: Home },
  { path: "/regist", name: "regist", component: Regist }, // 🔹 회원가입 라우트
  // 이후 /mypage 등도 여기에서 추가
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
