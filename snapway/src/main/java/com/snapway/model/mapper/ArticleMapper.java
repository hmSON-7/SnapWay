package com.snapway.model.mapper;

import java.util.List;

import com.snapway.model.dto.Article;

public interface ArticleMapper {

	List<Article> findAll();

	int saveArticle(Article article) throws Exception;

	Article getArticle(String articleId);

}
