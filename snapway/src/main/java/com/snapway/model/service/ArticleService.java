package com.snapway.model.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;
import com.snapway.model.dto.Reply;

public interface ArticleService {
	List<Article> findAll();

	void saveArticle(Article article) throws Exception;

	Article getArticle(long articleId) throws Exception;

	int addReply(Reply reply);

	List<Reply> getReply(long articleId);

	int deleteReply(int replyId, Authentication auth);

	int updateReply(Reply reply);

	int updateArticle(Article article) throws Exception;

	int deleteArticle(long articleId) throws Exception;
}
