package com.snapway.model.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;

public interface ArticleService {
	List<Article> findAll();

	void saveArticle(Article article) throws Exception;

	Article getArticle(String articleId) throws Exception;
}
