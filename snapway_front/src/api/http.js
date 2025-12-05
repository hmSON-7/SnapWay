import axios from "axios";

const baseURL = import.meta.env.VITE_API_BASE_URL;
const http = axios.create({
  baseURL: baseURL,
  timeout: 5000,
});

// TODO: JWT 사용 시 여기서 Authorization 헤더 인터셉터 설정
// http.interceptors.request.use(config => { ... })

export default http;
