import { createRouter, createWebHistory } from "vue-router";
import Home from "../views/home/Home.vue"; // 메인 페이지
import Regist from "../views/user/Regist.vue"; // 회원가입 페이지
import MyPage from "../views/user/MyPage.vue"; // 회원정보 페이지
import MapView from "../views/map/Map.vue"; // 관광지 조회 페이지
import BoardView from "../views/board/Board.vue"; // 게시판 페이지
import BoardWrite from "../views/board/BoardWrite.vue";
import BoardDetail from "../views/board/BoardDetail.vue";
import TravelRecord from "../views/record/TravelRecord.vue"; // 여행 기록 페이지

const routes = [
  { path: "/", name: "home", component: Home },
  { path: "/regist", name: "regist", component: Regist },
  { path: "/myPage", name: "myPage", component: MyPage },
  { path: "/map", name: "map", component: MapView },
  { path: "/board", name: "board", component: BoardView },
  { path: "/board/write", name: "boardWrite", component: BoardWrite },
  { path: "/board/:articleId", name: "boardDetail", component: BoardDetail },
  { path: "/board/:articleId/edit", name: "boardEdit", component: BoardWrite },
  { path: "/record", name: "record", component: TravelRecord },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;
