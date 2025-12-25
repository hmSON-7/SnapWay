package com.snapway.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

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

	@GetMapping("/articleList")
    public ResponseEntity<List<Article>> getArticleList(Authentication auth) { // Authentication 주입 받음
        
        Integer currentUserId = null;

        // 로그인 상태라면 userId 추출
        if (auth != null && auth.isAuthenticated()) {
            try {
                Map<?, ?> details = (Map<?, ?>) auth.getDetails();
                // Map에서 꺼낼 때 타입 캐스팅 주의 (보통 Integer or Double로 옴)
                Object userIdObj = details.get("userId");
                if (userIdObj instanceof Integer) {
                    currentUserId = (Integer) userIdObj;
                } else if (userIdObj instanceof Double) {
                    currentUserId = ((Double) userIdObj).intValue();
                }
            } catch (Exception e) {
                log.warn("사용자 ID 추출 실패 (비회원 처리): {}", e.getMessage());
            }
        }

        // 서비스에 userId(또는 null) 전달
        List<Article> articleList = aService.findAll(currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(articleList);
    }

	/*
	 * 게시글을 작성하면 db에 게시글을 등록하고 이미지 파일은 로컬 스토리지에 저장 저장 경로는 basePath/authorId/articleId/
	 */
	@PostMapping(value = "/saveArticle", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> saveArticle(@RequestParam("title") String title,
			@RequestParam("content") String content, @RequestParam("category") String category,
			@RequestParam(value = "tags", required = false) String tags,
			@RequestParam(value = "visibility", required = false, defaultValue = "PUBLIC") String visibility,
			@RequestPart(value = "image", required = false) MultipartFile image,
			Authentication auth) {

		log.debug("게시글 저장 요청 - title: {}, category: {}", title, category);

		if (auth == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "로그인이 필요합니다"));
		}

		try {
			Map<?, ?> details = (Map<?, ?>) auth.getDetails();
			int authorId = (int) details.get("userId");
			String realName = (String) details.get("realName");

			Article article = new Article();
			article.setTitle(title);
			article.setContent(content);
			article.setCategory(category);
			article.setTags(tags);
			article.setAuthorId(authorId);
			article.setAuthorName(realName);
			article.setVisibility(visibility);

			aService.saveArticle(article);

			return ResponseEntity.ok(Map.of("message", "게시글이 정상적으로 등록되었습니다"));
		} catch (Exception e) {
			log.error("게시글 등록 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "게시글 등록 중 오류가 발생했습니다"));
		}
	}


	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleMaxUploadSizeExceededException(
			MaxUploadSizeExceededException e) {
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
			HttpServletRequest request, Authentication auth)
			throws IllegalStateException, IOException {
		if (file == null || file.isEmpty()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("message", "업로드 실패 \n파일이 없습니다"));
		}

		if (auth == null) {
			log.error(
					"====================================auth 정보 null===============================");

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(Map.of("message", "로그인이 필요합니다"));
		}

		Map<?, ?> map = (Map<?, ?>) auth.getDetails();
		// forEach 람다 사용 - 가장 간결함
		map.forEach((key, value) -> {
			System.out.println("key: " + key + ", value: " + value);
		});

		int userId = (int) map.get("userId");
		String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();

		// 파일 저장 경로: basePath/userId/fileName
		Path savePath = Paths.get(basePath, String.valueOf(userId));
		Files.createDirectories(savePath);

		Path target = savePath.resolve(fileName);
		file.transferTo(target);

		// URL 생성 - 슬래시 추가 필수!
		String baseUrl = request.getScheme() + "://" + request.getServerName() + ":"
				+ (request.getServerPort() + 1);
		String fileUrl = baseUrl + "/" + userId + "/" + fileName; // 슬래시 추가!

		log.debug("이미지 업로드 완료: {}", fileUrl);

		return ResponseEntity.status(HttpStatus.OK).body(Map.of("fileUrl", fileUrl));
	}


	/*
	 * 클라이언트에게 게시글 전달
	 */
	@GetMapping("/article")
	public ResponseEntity<?> getArticle(@RequestParam long articleId) {
		try {
			Article article = aService.getArticle(articleId);
			List<Reply> replyList = aService.getReply(articleId);

			if (article == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(Map.of("message", "게시글을 찾을 수 없습니다"));
			}

			// JSON 형식으로 응답
			Map<String, Object> response = new HashMap<>();
			response.put("article", article);
			response.put("replies", replyList);

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("게시글 조회 오류: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "게시글 조회 중 오류가 발생했습니다"));
		}
	}
	
	/*
     * 게시글 수정 (PUT /api/article/article)
     * Body: { articleId: 1, title: "...", content: "...", ... }
     */
    @PutMapping("/article")
    public ResponseEntity<Map<String, String>> updateArticle(@RequestBody Article article, Authentication auth) {
        // 1. 로그인 확인
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다"));
        }

        try {
            // 2. 현재 로그인한 사용자 ID 가져오기
            Map<?, ?> details = (Map<?, ?>) auth.getDetails();
            int userId = (int) details.get("userId");

            // 3. 기존 게시글 확인 (존재 여부 & 권한 체크)
            // 주의: getArticle은 조회수를 증가시키므로, 단순 조회용 메서드가 있다면 그걸 쓰는 게 좋지만
            // 현재 로직상 getArticle을 재활용해도 기능엔 문제없음.
            Article existingArticle = aService.getArticle(article.getArticleId());
            
            if (existingArticle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "게시글이 존재하지 않습니다."));
            }

            // 본인 확인 (작성자 ID와 현재 로그인 ID 비교)
            if (existingArticle.getAuthorId() != userId) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
                         .body(Map.of("message", "수정 권한이 없습니다."));
            }

            // 4. 작성자 ID 강제 주입 (위변조 방지)
            article.setAuthorId(userId); 

            // 5. 업데이트 서비스 호출
            int result = aService.updateArticle(article);
            if (result != 1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "게시글 수정 실패"));
            }

            return ResponseEntity.ok(Map.of("message", "게시글이 수정되었습니다."));

        } catch (Exception e) {
            log.error("게시글 수정 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류 발생"));
        }
    }

    /*
     * 게시글 삭제 (DELETE /api/article/article?articleId=123)
     */
    @DeleteMapping("/article")
    public ResponseEntity<Map<String, String>> deleteArticle(@RequestParam long articleId, Authentication auth) {
         // 1. 로그인 확인
         if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "로그인이 필요합니다"));
        }

        try {
            // 2. 현재 로그인한 사용자 ID 가져오기
            Map<?, ?> details = (Map<?, ?>) auth.getDetails();
            int userId = (int) details.get("userId");
            
            // 관리자 권한 확인 (필요 시 주석 해제하여 사용)
            // String role = (String) details.get("roles"); // roles가 List인지 String인지 확인 필요

            // 3. 기존 게시글 확인
            Article existingArticle = aService.getArticle(articleId);
             if (existingArticle == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "게시글이 존재하지 않습니다."));
            }

            // 4. 권한 체크 (본인)
            // 만약 관리자도 삭제 가능하게 하려면 || 조건 추가
            if (existingArticle.getAuthorId() != userId) {
                 return ResponseEntity.status(HttpStatus.FORBIDDEN)
                         .body(Map.of("message", "삭제 권한이 없습니다."));
            }

            // 5. 삭제 서비스 호출
            int result = aService.deleteArticle(articleId);
             if (result != 1) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "게시글 삭제 실패"));
            }

            return ResponseEntity.ok(Map.of("message", "게시글이 삭제되었습니다."));

        } catch (Exception e) {
            log.error("게시글 삭제 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "서버 오류 발생"));
        }
    }

	@PostMapping("/addReply")
	public ResponseEntity<Map<String, ?>> addReply(@RequestBody Reply reply, Authentication auth) {
		Map<?, ?> details = (Map<?, ?>) auth.getDetails();
		int replierId = (int) details.get("userId");
		String replierName = (String) details.get("realName");
		reply.setReplierId(replierId);
		reply.setReplierName(replierName);

		int result = aService.addReply(reply);

		if (result != 1) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "댓글을 등록할 수 없습니다."));
		}

		return ResponseEntity.ok(Map.of("message", "댓글 등록 완료"));
	}

	@DeleteMapping("/deleteReply")
	public ResponseEntity<Map<String, ?>> deleteReply(@RequestBody Map<String, ?> req,
			Authentication auth) {
		int replyId = (int) req.get("replyId");
		int result = aService.deleteReply(replyId, auth);

		if (result != 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "댓글 삭제 실패"));
		}

		return ResponseEntity.ok(Map.of("message", "댓글 삭제 성공"));
	}

	@PutMapping("/updateReply")
	public ResponseEntity<Map<String, String>> updateReply(@RequestBody Reply reply,
			Authentication auth) {
		int replyId = reply.getReplyId();
		String content = reply.getContent();
		int replierId = (int) ((Map<?, ?>) auth.getDetails()).get("userId");
		reply.setReplierId(replierId);

		int result = aService.updateReply(reply);

		if (result != 1) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "댓글 수정 실패"));
		}

		return ResponseEntity.ok(Map.of("message", "댓글 수정 성공"));
	}

}
