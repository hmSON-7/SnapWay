package com.snapway.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;
import com.snapway.model.service.ArticleService;
import com.snapway.util.FileUtil;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {
	@Value("${spring.servlet.multipart.max-file-size}")
	private String maxSize;
	@Value("${spring.servlet.multipart.max-request-size}")
	private String totalMaxSize;
	private final ArticleService aService;
	
	@GetMapping
	public ResponseEntity<List<Article>> getArticleList() {
		List<Article> articleList = aService.findAll();
		for(Article a : articleList) {
			System.out.println("확인용:" + a.toString());
		}
		return ResponseEntity.status(HttpStatus.OK).body(articleList);
	}
	
	
	/*
	 * 게시글을 작성하면 db에 게시글을 등록하고 이미지 파일은 라즈베리 파이에 저장
	 * 저장 경로는 /home/snapway/files/게시글id/이미지파일이름
	 */
	@PostMapping(value="", consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> createArticle(
	        @RequestPart("article") Article article,
	        @RequestPart(value="files", required=false) List<MultipartFile> files) {
	    try {
	    	
	    	//TODO: 나중에 엑세스 토큰 구현되면 거기서 작성자 정보 추출
	    	article.setAuthorId("1");
	        aService.createArticle(article, files);
	        return ResponseEntity.ok(Map.of("message", "게시글이 정상적으로 등록되었습니다"));
	    } catch(Exception e) {
	        log.error("게시글 등록 오류", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(Map.of("message", e.getMessage()));
	    }
	}
	
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException e
	) {
		log.error("파일 크기 제한 초과", e);
		String message;
		String causeMessage = e.getRootCause().getMessage();
		if(causeMessage.contains("FileSizeLimitExceeded"))
			message = String.format("개별 파일의 크기는 최대 %sMB입니다.", maxSize);
		else 
			message = String.format("전체 파일의 크기는 최대 %sMB입니다", totalMaxSize);
		
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message", message));
		
	}

}
