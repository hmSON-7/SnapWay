import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/Home.vue"; // 메인 페이지
import Regist from "../views/Regist.vue"; // 회원가입 페이지
import MyPage from "../views/MyPage.vue"; // 회원정보 페이지
import MapView from "../views/Map.vue"; // 관광지 조회 페이지
import BoardView from "../views/Board.vue"; // 게시판 페이지
import TravelRecord from "../views/TravelRecord.vue"; // 여행 기록 페이지

const routes = [
  { path: "/", name: "home", component: Home },
  { path: "/regist", name: "regist", component: Regist },
  { path: "/myPage", name: "myPage", component: MyPage },
  { path: "/map", name: "map", component: MapView },
  { path: "/board", name: "board", component: BoardView },
  { path: "/record", name: "record", component: TravelRecord },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
