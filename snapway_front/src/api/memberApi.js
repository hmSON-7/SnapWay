// src/api/memberApi.js
import http from "./http";

// 로그인
export const loginMember = (email, password) => {
  return http.post("/member/login", { email, password });
};

// 로그아웃
export const logoutMember = () => {
  return http.post("/member/logout", {});
};

// 마이페이지 진입 시 회원정보 조회
export const fetchMyInfo = (member) => {
  return http.get("/member/fetchMyInfo", member);
};

// 회원가입
export const registMember = (member) => {
  return http.post("/member/regist", member);
};
