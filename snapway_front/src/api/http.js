import axios from "axios";

const http = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 5000,
});

// TODO: JWT 사용 시 여기서 Authorization 헤더 인터셉터 설정
// http.interceptors.request.use(config => { ... })

export default http;
