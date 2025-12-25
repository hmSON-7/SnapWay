package com.snapway.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.snapway.model.dto.Article;
import com.snapway.model.dto.Member;
import com.snapway.model.service.ArticleService;
import com.snapway.model.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
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
	private final MemberService memberService;
	@Value("${spring.servlet.multipart.location}")
	private String basePath;

	@GetMapping("/articleList")
	public ResponseEntity<List<Article>> getArticleList() {
		List<Article> articleList = aService.findAll();
		for (Article a : articleList) {
			System.out.println("확인용:" + a.toString());
		}
		return ResponseEntity.status(HttpStatus.OK).body(articleList);
	}

	
	
	/*
	 * 게시글을 작성하면 db에 게시글을 등록하고 이미지 파일은 라즈베리 파이에 저장
	 * 저장 경로는 /home/snapway/files/게시글id/이미지파일이름
	 */
	@PostMapping(value="", consumes= MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> saveArticle(
			@RequestPart("article") Article article,
			@RequestPart(value = "files", required = false) List<MultipartFile> files,
			Authentication authentication) {
	    try {
	    	if (authentication == null) {
	    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	    				.body(Map.of("message", "로그인이 필요합니다."));
	    	}
	    	String email = authentication.getName();
	    	Member loginUser;
	    	try {
	    		loginUser = memberService.getMemberInfo(email);
	    	} catch (Exception e) {
	    		log.error("회원 조회 오류", e);
	    		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	    				.body(Map.of("message", "서버 오류"));
	    	}
	    	if (loginUser == null) {
	    		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	    				.body(Map.of("message", "로그인이 필요합니다."));
	    	}
	    	article.setAuthorId(loginUser.getId());
	        aService.saveArticle(article, files);
	        return ResponseEntity.ok(Map.of("message", "게시글이 정상적으로 등록되었습니다."));
	    } catch(Exception e) {
	        log.error("게시글 등록 오류", e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	            .body(Map.of("message", e.getMessage()));
	    }
	}
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException e) {
		log.error("파일 크기 제한 초과", e);
		String message;
		String causeMessage = e.getRootCause().getMessage();
		if(causeMessage.contains("FileSizeLimitExceeded"))
			message = String.format("개별 파일의 크기는 최대 %sMB입니다.", maxSize);
		else 
			message = String.format("전체 파일의 크기는 최대 %sMB입니다", totalMaxSize);
		
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("message", message));
		
	}


	/*
	 * 클라이언트의 에디터가 보낸 이미지를 저장하고 그 url을 다시 에디터에 반환
	 */
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, ?>> uploadImage(@RequestPart("file") MultipartFile file,
			@RequestParam(value = "userId", required = false) Integer userId,
			HttpServletRequest request,
			Authentication authentication)
			throws IllegalStateException, IOException {
		if (authentication == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "로그인이 필요합니다."));
		}
		String email = authentication.getName();
		Member loginUser;
		try {
			loginUser = memberService.getMemberInfo(email);
		} catch (Exception e) {
			log.error("회원 조회 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "서버 오류"));
		}
		if (loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "로그인이 필요합니다."));
		}
		if (file == null || file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "업로드 실패 \n파일이 없습니다"));
		}

		String userDir = String.valueOf(loginUser.getId());
		String fileName = UUID.randomUUID().toString() + file.getOriginalFilename();

		// 경로 예시 c:/user/uploads/userId/temp/fileName
		Path savePath = Paths.get(basePath, userDir, "temp");

		// 이미지를 저장할 디렉토리를 생성
		Files.createDirectories(savePath);

		Path target = savePath.resolve(fileName);
		file.transferTo(target);

		// 프론트에 접근 가능한 이미지url 구성
		String baseUrl = request.getScheme() + "://" + request.getServerName() + ":"
				+ request.getServerPort() + request.getContextPath();
		String fileUrl = baseUrl + "/files/" + userDir + "/" +  "temp" + "/" + fileName;

		return ResponseEntity.status(HttpStatus.OK).body(Map.of("fileUrl", fileUrl));
	}
	@GetMapping("/article")
	public ResponseEntity<Map<String, ?>> getArticle(@RequestParam long articleId) {
		try {
			Article article = aService.getArticle(articleId);

			if (article == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("message", "해당 게시글이 존재하지 않습니다."));
			}

			return ResponseEntity.status(HttpStatus.OK).body(Map.of("article", article));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "서버 오류"));
		}
	}

	@PutMapping("/article")
	public ResponseEntity<Map<String, String>> updateArticle(@RequestBody Article article) {
		try {
			if (article.getCategory() == null || article.getCategory().isBlank()) {
				article.setCategory("자유");
			}
			int result = aService.updateArticle(article);
			if (result == 1) {
				return ResponseEntity.ok(Map.of("message", "게시글이 정상적으로 수정되었습니다"));
			}
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", "게시글이 존재하지 않습니다"));
		} catch (Exception e) {
			log.error("게시글 수정 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", e.getMessage()));
		}
	}

	@DeleteMapping("/article")
	public ResponseEntity<Map<String, String>> deleteArticle(@RequestParam long articleId) {
		try {
			int result = aService.deleteArticle(articleId);
			if (result == 1) {
				return ResponseEntity.ok(Map.of("message", "게시글이 정상적으로 삭제되었습니다"));
			}
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("message", "게시글이 존재하지 않습니다"));
		} catch (Exception e) {
			log.error("게시글 삭제 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", e.getMessage()));
		}
	}

}
