package com.snapway.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.snapway.model.dto.Article;

@Mapper
public interface ArticleMapper {

	List<Article> findAll();

	int saveArticle(Article article) throws Exception;

	Article getArticle(long articleId) throws Exception;

	int updateArticle(Article article) throws Exception;

	int deleteArticle(long articleId) throws Exception;

	int increaseHits(long articleId) throws Exception;

}
