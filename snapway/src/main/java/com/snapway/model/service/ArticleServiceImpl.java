package com.snapway.model.service;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;
import com.snapway.model.mapper.ArticleMapper;
import com.snapway.util.FileUtil;

import lombok.AllArgsConstructor;

import java.io.*;

@Service
@AllArgsConstructor
public class ArticleServiceImpl implements ArticleService {
	private final ArticleMapper aMapper;
	private final FileUtil fileUtil;

	@Override
	public List<Article> findAll() {
		return aMapper.findAll();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void createArticle(Article article, List<MultipartFile> files) throws Exception {
		int result = aMapper.createArticle(article);
		if(result != 1) 
			throw new RuntimeException("게시글 등록 실패");
		
		if(files != null && !files.isEmpty())
			fileUtil.saveMultipartFile(files, article.getAuthorId(), article.getArticleId());
		
	}

}
