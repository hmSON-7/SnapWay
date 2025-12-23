package com.snapway.controller;

import java.util.*;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapway.model.service.MemberService;
import com.snapway.security.JwtUtil;
import com.snapway.util.FileUtil;

import jakarta.servlet.http.HttpSession;

import com.snapway.model.dto.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;
	private final FileUtil fileUtil;
	private final JwtUtil jwtUtil;

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
	public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> map) {
		String email = map.get("email");
		String password = map.get("password");

		log.debug("로그인 요청: email={}", email);

		Map<String, Object> resultMap = new HashMap<>();
		HttpStatus status = null;

		try {
			// 1. email, pw로 사용자 인증
			Member loginMember = memberService.loginMember(email, password);
			
			if(loginMember != null) { // 로그인 성공 시
				// 2. 권한 리스트 생성
				List<String> roles = new ArrayList<>();
				roles.add(loginMember.getRole().name());
				
				// 3. 토큰 생성
				String accessToken = jwtUtil.generateAccessToken(email, roles);
				String refreshToken = jwtUtil.generateRefreshToken(email);
				
				// 4. 클라이언트에게 전달할 데이터 생성
				resultMap.put("userInfo", loginMember);
				resultMap.put("accessToken", accessToken);
				resultMap.put("refreshToken", refreshToken);
				resultMap.put("message", "success");
				status = HttpStatus.OK;
			}
			else { // 로그인 실패 시
				resultMap.put("message", "fail");
				status = HttpStatus.UNAUTHORIZED;
			}
		} catch (Exception e) {
			log.error("로그인 에러: {}", e.getMessage());
			resultMap.put("message", e.getMessage());
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
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
    public ResponseEntity<String> logout() {
        log.debug("로그아웃 요청 - 클라이언트 토큰 삭제 필요");
        return ResponseEntity.ok("success");
    }

	/**
     * 내 정보 조회 (JWT 기반)
     */
    @GetMapping("/fetchMyInfo")
    public ResponseEntity<?> fetchMyInfo(Authentication authentication) {
    	log.debug("마이페이지 접근 요청");
        // Authentication 객체가 null이면 필터에서 걸러졌거나 인증 실패 상태
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        log.debug("인증 성공");
        try {
            String email = authentication.getName(); // JWT Payload의 Subject(email) 추출
            Member member = memberService.getMemberInfo(email);
            
            if (member == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("회원 정보를 찾을 수 없습니다.");
            }
            return ResponseEntity.ok(member);
            
        } catch (Exception e) {
            log.error("정보 조회 에러: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("에러 발생");
        }
    }
	
    /**
     * 회원 정보 수정 (PUT /api/member/update)
     */
    @PutMapping("/update")
    public ResponseEntity<Map<String, Object>> updateMember(@RequestBody Member member, Authentication authentication) {
        Map<String, Object> resultMap = new HashMap<>();
        
        if (authentication == null) {
            resultMap.put("message", "unauthorized");
            return new ResponseEntity<>(resultMap, HttpStatus.UNAUTHORIZED);
        }

        try {
            // 1. 토큰에서 이메일 추출
            String email = authentication.getName();
            
            // 2. DB에서 현재 사용자 정보 조회 (PK인 id를 얻기 위함)
            Member dbMember = memberService.getMemberInfo(email);
            if (dbMember == null) {
                resultMap.put("message", "user_not_found");
                return new ResponseEntity<>(resultMap, HttpStatus.NOT_FOUND);
            }

            // 3. 수정할 객체에 ID와 이메일 강제 주입 (변조 방지)
            member.setId(dbMember.getId());
            member.setEmail(email);

            // 4. 정보 수정 요청
            int result = memberService.updateMember(member);
            
            if (result > 0) {
                resultMap.put("message", "success");
                return new ResponseEntity<>(resultMap, HttpStatus.OK);
            } else {
                resultMap.put("message", "fail");
                return new ResponseEntity<>(resultMap, HttpStatus.BAD_REQUEST);
            }
            
        } catch (Exception e) {
            log.error("회원 수정 에러: ", e);
            resultMap.put("message", "error");
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * 회원 탈퇴 (DELETE /api/member/{email})
     */
    @DeleteMapping("/{email}")
    public ResponseEntity<Map<String, Object>> deleteMember(@PathVariable String email, Authentication authentication) {
        Map<String, Object> resultMap = new HashMap<>();
        
        if (authentication == null) {
            resultMap.put("message", "unauthorized");
            return new ResponseEntity<>(resultMap, HttpStatus.UNAUTHORIZED);
        }

        // 본인 확인: 토큰의 이메일과 삭제 요청한 이메일이 일치하는지 확인
        String tokenEmail = authentication.getName();
        if (!tokenEmail.equals(email)) {
             resultMap.put("message", "forbidden");
             return new ResponseEntity<>(resultMap, HttpStatus.FORBIDDEN);
        }

        try {
            int result = memberService.deleteMember(email);
            
            if (result > 0) {
                resultMap.put("message", "success");
                return new ResponseEntity<>(resultMap, HttpStatus.OK);
            } else {
                resultMap.put("message", "fail");
                return new ResponseEntity<>(resultMap, HttpStatus.BAD_REQUEST);
            }
        } catch (Exception e) {
            log.error("회원 탈퇴 에러: ", e);
            resultMap.put("message", "error");
            return new ResponseEntity<>(resultMap, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}