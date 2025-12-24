package com.snapway.model.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.security.core.Authentication;

import com.snapway.model.dto.Article;
import com.snapway.model.dto.Reply;

@Mapper
public interface ArticleMapper {

	List<Article> findAll();


	int saveArticle(@Param("article") Article article) throws Exception;
	
	Article getArticle(@Param("articleId")long articleId) throws Exception;

	int addReply(@Param("reply")Reply reply);

	List<Reply> getReply(@Param("articleId") long articleId);

	int deleteReply(@Param("replyId")int replyId, @Param("replierId")int replierId);

	int updateReply(@Param("reply")Reply reply);

	int updateArticle(Article article) throws Exception;

	int deleteArticle(long articleId) throws Exception;

	int increaseHits(long articleId) throws Exception;


}
