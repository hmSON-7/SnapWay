package com.snapway.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.snapway.member.model.service.MemberService;
import com.snapway.member.model.dto.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 회원가입 (POST /api/member/regist)
     * @param member JSON 형태로 전달받은 회원 정보
     * @return 성공 시 "success" 메시지, 실패 시 에러 메시지
     */
    @PostMapping("/regist")
    public ResponseEntity<Map<String, Object>> regist(@RequestBody Member member) {
        log.debug("회원가입 요청: {}", member);
        Map<String, Object> resultMap = new HashMap<>();
        HttpStatus status = null;

        try {
            int result = memberService.registMember(member);
            if (result == 1) {
                resultMap.put("message", "success");
                status = HttpStatus.CREATED;
            } else {
                resultMap.put("message", "fail");
                status = HttpStatus.BAD_REQUEST;
            }
        } catch (Exception e) {
            log.error("회원가입 에러: {}", e.getMessage());
            resultMap.put("message", e.getMessage());
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return new ResponseEntity<>(resultMap, status);
    }

    /**
     * 로그인 (POST /api/member/login)
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
            Member loginMember = memberService.loginMember(email, password);
            if (loginMember != null) {
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

        return new ResponseEntity<>(resultMap, status);
    }

    /**
     * 이메일 중복 체크 (GET /api/member/check-email)
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
    
    // 로그아웃은 JWT 미사용 시 클라이언트(Vue)에서 저장된 정보 삭제로 처리하거나,
    // 세션 사용 시 session.invalidate() 호출하는 엔드포인트를 만들면 됩니다.
}