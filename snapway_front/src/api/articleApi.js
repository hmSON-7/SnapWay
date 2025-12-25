// src/api/articleApi.js
import http from "./http";

export const fetchArticles = () => http.get("/article/articleList");

export const fetchArticle = (articleId) =>
  http.get("/article/article", { params: { articleId } });

export const createArticle = (formData) => {
  const accessToken = localStorage.getItem("accessToken");

  return http.post("/article/saveArticle", formData, {
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });
};


export const updateArticle = (article) => http.put("/article/article", article);

export const deleteArticle = (articleId) =>
  http.delete("/article/article", { params: { articleId } });

export const uploadArticleImage = (file, userId) => {
  const formData = new FormData();
  formData.append("file", file);
  return http.post("/article/upload", formData, {
    params: userId ? { userId } : undefined,
    headers: { "Content-Type": "multipart/form-data" },
  });
};
