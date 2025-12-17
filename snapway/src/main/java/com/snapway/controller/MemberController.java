package com.snapway.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapway.model.service.MemberService;
import com.snapway.util.FileUtil;

import jakarta.servlet.http.HttpSession;

import com.snapway.model.dto.Member;
import com.snapway.model.dto.Role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;
	private final FileUtil fileUtil;

	/**
	 * 회원가입 (POST /api/member/regist)
	 * 
	 * @param member JSON 형태로 전달받은 회원 정보
	 * @return 성공 시 "success" 메시지, 실패 시 에러 메시지
	 */
	@PostMapping("/regist")
	public ResponseEntity<Map<String, Object>> regist(@RequestBody Member member) {
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status;


		try {
			// 0. 이메일 중복 체크 (idCheck가 true면 중복, false면 사용 가능)
			boolean isDuplicated = memberService.idCheck(member.getEmail());
			if (isDuplicated) {
				resultMap.put("message", "duplicate_email");
				status = HttpStatus.CONFLICT; // 409
				return new ResponseEntity<>(resultMap, status);
			}

			// 1. 회원가입 시도
			int result = memberService.registMember(member);
			
			// 사용자의 id로 된 경로를 서버에 생성.
			fileUtil.createUserDirectory(member.getId());

			if (result == 1) {
				// 회원가입 성공
				resultMap.put("message", "success");
				status = HttpStatus.CREATED; // 201
			} else {
				// insert 실패(유효성 등)
				resultMap.put("message", "validation_fail");
				status = HttpStatus.BAD_REQUEST; // 400
			}

		} catch (Exception e) {
			log.error("회원가입 에러: ", e);
			resultMap.put("message", "internal_error");
			status = HttpStatus.INTERNAL_SERVER_ERROR; // 500
		}

		return new ResponseEntity<>(resultMap, status);
	}

	/**
	 * 로그인 (POST /api/member/login)
	 * 
	 * @param map JSON (email, password)
	 * @return 로그인 성공 시 회원 정보(비밀번호 제외), 실패 시 에러 메시지
	 */
	@PostMapping("/login")
	public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> map, HttpSession session) {
		String email = map.get("email");
		String password = map.get("password");

		log.debug("로그인 요청: email={}", email);

		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;

		// 일단 임시로 로그인 됬다고 가정하고 진행

		try {
			Member loginMember = memberService.loginMember(email, password);
			if (loginMember != null) {
				// 일단 세션 쿠키 방식으로 로그인 인증
				session.setAttribute("loginUser", loginMember);
				// 로그인 성공 -> JWT 토큰 발급 로직이 들어갈 자리 (현재는 회원 정보만 반환)
				resultMap.put("userInfo", loginMember);
				resultMap.put("message", "success");
				status = HttpStatus.OK;
			} else {
				resultMap.put("message", "fail");
				status = HttpStatus.UNAUTHORIZED; // 401 Unauthorized
			}
		} catch (Exception e) {
			log.error("로그인 에러: {}", e.getMessage());
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		/*
		 * // 1. 테스트용 가짜 회원 생성 (실제로는 DB 조회 결과가 들어올 자리) Member dummyMember =
		 * Member.builder() .id(1) .email(email) .username("테스트사용자") .role(Role.USER) //
		 * enum이면 적절히 .profileImg(null) .gender(null) .birthday(null) .style(null)
		 * .build();
		 * 
		 * // 2. 세션에 로그인 정보 저장 session.setAttribute("loginUser", dummyMember);
		 * 
		 * // 3. 프론트로 내려줄 요약 정보 (비밀번호 제외) resultMap.put("userInfo", dummyMember);
		 * resultMap.put("message", "success"); status = HttpStatus.OK;
		 */
		System.out.println("로그인 성공~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		return new ResponseEntity<>(resultMap, status);
	}

	/**
	 * 이메일 중복 체크 (GET /api/member/check-email)
	 * 
	 * @param email 체크할 이메일
	 * @return 사용 가능 여부 (true: 사용 가능, false: 중복)
	 */
	@GetMapping("/check-email")
	public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {
		log.debug("이메일 중복 체크 요청: {}", email);
		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;

		try {
			// idCheck가 true면 중복(사용불가), false면 사용 가능
			boolean isDuplicated = memberService.idCheck(email);

			if (isDuplicated) {
				resultMap.put("isAvailable", false);
				resultMap.put("message", "이미 사용 중인 이메일입니다.");
			} else {
				resultMap.put("isAvailable", true);
				resultMap.put("message", "사용 가능한 이메일입니다.");
			}
			status = HttpStatus.OK;
		} catch (Exception e) {
			log.error("중복 체크 에러: {}", e.getMessage());
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}

		return new ResponseEntity<>(resultMap, status);
	}

	@PostMapping("/logout")
	public void logout(HttpSession session) {
		session.invalidate();
		System.out.println("로그아웃 성공~~~~~~~~~~~~~~~~~~~~~~~~~~~");
	}

	@GetMapping("/fetchMyInfo")
	public ResponseEntity<?> fetchMyInfo(HttpSession session) {
		Member loginUser = (Member) session.getAttribute("loginUser");

		if (loginUser == null) {
			// 세션에 로그인 정보 없음 → 401 (로그인 필요)
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
		}

		return ResponseEntity.ok(loginUser);
	}

}