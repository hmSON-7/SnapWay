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

// 회원가입
export const registMember = () => {
  return http.post("/member/regist", {});
};
