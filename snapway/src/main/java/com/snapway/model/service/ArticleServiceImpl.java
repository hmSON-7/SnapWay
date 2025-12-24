package com.snapway.model.service;

import java.util.*;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;
import com.snapway.model.dto.Reply;
import com.snapway.model.mapper.ArticleMapper;
import com.snapway.util.FileUtil;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
	private final ArticleMapper aMapper;
	
	@Value("${spring.servlet.multipart.location}")
	private String basePath;

	@Override
	public List<Article> findAll() {
		return aMapper.findAll();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void saveArticle(Article article) throws Exception {
		int result = aMapper.saveArticle(article);
		if (result != 1)
			throw new RuntimeException("게시글 등록 실패");
		
		int userId = article.getAuthorId();
		long articleId = article.getArticleId();
		
		
		// 이미지 파일을 temp에서 articleId폴더로 이동
		Path tempDir = Paths.get(basePath, String.valueOf(userId), "temp");
		Path articleDir = Paths.get(basePath, String.valueOf(userId), String.valueOf(articleId));
		
		// 저장할 디렉토리 생성
		Files.createDirectories(articleDir);
		
		// temp 디렉토리 안의 이미지 파일을 articleDir로 이동
		try(Stream<Path> stream = Files.list(tempDir)) {
			stream
				.filter(Files::isRegularFile)
				.forEach(source->{ // 일반 파일이 맞으면 그 파일을 이동시킨다.
					Path target = articleDir.resolve(source.getFileName());
					try {
						Files.move(source, target);
					} catch(Exception e) {
						throw new RuntimeException("이미지 이동 실패:" + source, e);
					}
				});
		}

	}

	@Override
	public Article getArticle(long articleId) throws Exception {
		return aMapper.getArticle(articleId);
	}

	@Override
	public int addReply(Reply reply) {
		return aMapper.addReply(reply);
	}

	@Override
	public List<Reply> getReply(long articleId) {
		return aMapper.getReply(articleId);
	}

	@Override
	public int deleteReply(int replyId, Authentication auth) {
		int replierId = (int) ((Map<?, ?>) auth.getDetails()).get("userId");
		return aMapper.deleteReply(replyId, replierId);
	}

	@Override
	public int updateReply(Reply reply) {
		return aMapper.updateReply(reply);
	}

}
