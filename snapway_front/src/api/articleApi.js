// src/api/articleApi.js
import http from "./http";

export const fetchArticles = () => http.get("/article/articleList");

export const fetchArticle = (articleId) =>
  http.get("/article/article", { params: { articleId } });

export const createArticle = (formData) =>
  http.post("/article", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });

export const updateArticle = (article) =>
  http.put("/article/article", article);

export const deleteArticle = (articleId) =>
  http.delete("/article/article", { params: { articleId } });

export const uploadArticleImage = (file) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/article/upload", formData, {
    headers: { "Content-Type": "multipart/form-data" },
  });
};
