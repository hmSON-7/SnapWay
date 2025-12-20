package com.snapway.model.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;

public interface ArticleService {
	List<Article> findAll();

	void createArticle(Article article, List<MultipartFile> files) throws Exception;
}
