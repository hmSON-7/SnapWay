package com.snapway.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snapway.model.dto.Article;
import com.snapway.model.dto.Reply;
import com.snapway.model.service.ArticleService;

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

	@Value("${spring.servlet.multipart.location}")
	private String basePath;

	private final ObjectMapper objectMapper;

	@GetMapping("/articleList")
	public ResponseEntity<List<Article>> getArticleList() {
		List<Article> articleList = aService.findAll();
		for (Article a : articleList) {
			System.out.println("확인용:" + a.toString());
		}
		return ResponseEntity.status(HttpStatus.OK).body(articleList);
	}

	/*
	 * 게시글을 작성하면 db에 게시글을 등록하고 이미지 파일은 로컬 스토리지에 저장 저장 경로는
	 * basePath/authorId/articleId/
	 */
	@PostMapping(value = "saveArticle")
	public ResponseEntity<Map<String, String>> saveArticle(@RequestBody() Article article, Authentication auth) {
		if (auth == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다"));
		}

		try {
			// 1. 작성자 ID 설정 (항상 토큰 기준)
			Map<?, ?> details = (Map<?, ?>) auth.getDetails();
			int authorId = (int) details.get("userId");
			article.setAuthorId(authorId);

			// 2. 게시글 저장 (DB insert) 후 articleId 생성
			// aService.saveArticle가 articleId를 세팅해 주도록 구현
			aService.saveArticle(article); // files 인자 제거
			Long articleId = article.getArticleId();

			// 3. temp 디렉토리 → 최종 디렉토리로 이미지 이동
			Path tempDir = Paths.get(basePath, String.valueOf(authorId), "temp");
			Path targetDir = Paths.get(basePath, String.valueOf(authorId), String.valueOf(articleId));

			Files.createDirectories(targetDir);

			if (Files.exists(tempDir)) {
				try (var paths = Files.list(tempDir)) {
					paths.filter(Files::isRegularFile).forEach(sourcePath -> {
						try {
							Path targetPath = targetDir.resolve(sourcePath.getFileName());
							Files.move(sourcePath, targetPath /* , StandardCopyOption.REPLACE_EXISTING */);
						} catch (IOException e) {
							throw new RuntimeException("이미지 이동 실패: " + sourcePath.getFileName(), e);
						}
					});
				}

				// temp 폴더 비었으면 삭제 (선택)
				try (var paths = Files.list(tempDir)) {
					if (paths.findAny().isEmpty()) {
						Files.delete(tempDir);
					}
				}
			}

			return ResponseEntity.ok(Map.of("message", "게시글이 정상적으로 등록되었습니다"));
		} catch (Exception e) {
			log.error("게시글 등록 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "게시글 등록 중 오류가 발생했습니다"));
		}
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
		log.error("파일 크기 제한 초과", e);
		String message;
		String causeMessage = e.getRootCause().getMessage();
		if (causeMessage.contains("FileSizeLimitExceeded"))
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
			HttpServletRequest request, Authentication auth) throws IllegalStateException, IOException {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "업로드 실패 \n파일이 없습니다"));
		}

		if (auth == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다"));
		}

		Map<?, ?> map = (Map<?, ?>) auth.getDetails();
		int userId = (int) map.get("userId");
		String fileName = UUID.randomUUID().toString() + file.getOriginalFilename();

		// 경로예시 c:user\\uploads\\userId\temp\fileName
		Path savePath = Paths.get(basePath, String.valueOf(userId), "temp");

		// 이미지를 저장할 디렉토리를 생성
		Files.createDirectories(savePath);

		Path target = savePath.resolve(fileName);
		file.transferTo(target);

		// 에디터에게 접근 가능한 이미지url을 구성
		String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
				+ request.getContextPath();
		String fileUrl = baseUrl + "/files/" + userId + "/" + "temp" + "/" + fileName;

		return ResponseEntity.status(HttpStatus.OK).body(Map.of("fileUrl", fileUrl));
	}

	/*
	 * 클라이언트에게 게시판 전달
	 */
	@GetMapping(value = "/article", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MultiValueMap<String, Object>> getArticleAsMultipart(@RequestParam long articleId) {
		try {
			Article article = aService.getArticle(articleId);
			List<Reply> replyList = aService.getReply(articleId);
			if (article == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}

			// 1. article JSON 파트
			String articleJson = objectMapper.writeValueAsString(article);

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

			HttpHeaders articleHeaders = new HttpHeaders();
			articleHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> articlePart = new HttpEntity<>(articleJson, articleHeaders);
			body.add("article", articlePart); // part name: article

			// 1-2. replyList JSON 파트 추가
			String replyListJson = objectMapper.writeValueAsString(replyList); // List<Reply> -> JSON 배열 문자열[web:29]
			HttpHeaders replyHeaders = new HttpHeaders();
			replyHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> replyPart = new HttpEntity<>(replyListJson, replyHeaders);
			body.add("replies", replyPart); // part name: replies

			// 2. content 안의 <img src="...">에서 파일들 파싱
			String content = article.getContent();
			Pattern pattern = Pattern.compile("<img\\s+[^>]*src=\\\"([^\\\"]+)\\\"[^>]*>");
			Matcher matcher = pattern.matcher(content);

			long authorId = article.getAuthorId();

			while (matcher.find()) {
				String src = matcher.group(1); // http://localhost:8081/files/8/temp/xxx.png
				String fileName = src.substring(src.lastIndexOf('/') + 1);

				Path imagePath = Paths.get(basePath, String.valueOf(authorId), String.valueOf(articleId), fileName);

				if (!Files.exists(imagePath)) {
					continue;
				}

				FileSystemResource fileResource = new FileSystemResource(imagePath.toFile());

				HttpHeaders fileHeaders = new HttpHeaders();
				// MIME 타입 세팅
				String mimeType = Files.probeContentType(imagePath);
				MediaType mediaType = (mimeType != null) ? MediaType.parseMediaType(mimeType)
						: MediaType.APPLICATION_OCTET_STREAM;
				fileHeaders.setContentType(mediaType);
				fileHeaders.setContentDisposition(
						ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());

				HttpEntity<Resource> filePart = new HttpEntity<>(fileResource, fileHeaders);

				// 같은 이름으로 여러 파일을 보내고 싶으면 모두 "files"로 add
				body.add("files", filePart);
				// 또는 각자 다른 이름을 원하면 body.add("file-" + fileName, filePart);
			}

			return ResponseEntity.ok().contentType(MediaType.MULTIPART_FORM_DATA).body(body);

		} catch (Exception e) {
			log.error("게시글 멀티파트 응답 오류: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/addReply")
	public ResponseEntity<Map<String, ?>> addReply(@RequestBody Reply reply, Authentication auth) {
		Map<?, ?> details = (Map<?, ?>) auth.getDetails();
		int replierId = (int) details.get("userId");
		reply.setReplierId(replierId);

		int result = aService.addReply(reply);

		if (result != 1) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "댓글을 등록할 수 없습니다."));
		}

		return ResponseEntity.ok(Map.of("message", "댓글 등록 완료"));
	}
	
	@DeleteMapping("/deleteReply")
	public ResponseEntity<Map<String, ?>> deleteReply(@RequestBody Map<String, ?> req, Authentication auth) {
		int replyId = (int) req.get("replyId");
		int result = aService.deleteReply(replyId, auth);
		
		if(result != 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "댓글 삭제 실패"));
		}
		
		return ResponseEntity.ok(Map.of("message", "댓글 삭제 성공"));
	}
	
	@PutMapping("/updateReply")
	public ResponseEntity<Map<String, String>> updateReply(@RequestBody Reply reply, Authentication auth) {
		int replyId = reply.getReplyId();
		String content = reply.getContent();
		int replierId = (int) ((Map<?, ?>) auth.getDetails()).get("userId");
		reply.setReplierId(replierId);
		
		int result = aService.updateReply(reply);
		
		if(result != 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "댓글 수정 실패"));
		}
		
		return ResponseEntity.ok(Map.of("message", "댓글 수정 성공"));
	}

}