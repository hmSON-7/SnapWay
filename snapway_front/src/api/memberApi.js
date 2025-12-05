// src/api/memberApi.js
import http from "./http";

// 로그인
export const loginMember = (email, password) => {
  return http.post("/member/login", { email, password });
};

// 회원가입
export const registMember = () => {
  return http.post("member/regist", {});
};

// 이후 회원가입, 내 정보 조회 등도 여기서 확장
// export const registMember = (member) => http.post('/member/regist', member)
// export const getMyInfo = () => http.get('/member/me')
