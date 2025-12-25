package com.snapway.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.snapway.model.dto.AuthDto;
import com.snapway.model.service.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	
	// 1. 인증 번호 요청
    @PostMapping("/password/code-request")
    public ResponseEntity<?> sendCode(@RequestBody AuthDto.EmailRequest request) {
        try {
            authService.sendVerificationCode(request.getEmail());
            return ResponseEntity.ok(Map.of("message", "인증 번호가 발송되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "메일 발송 중 오류가 발생했습니다."));
        }
    }

    // 2. 인증 번호 검증
    @PostMapping("/password/code-verify")
    public ResponseEntity<?> verifyCode(@RequestBody AuthDto.VerifyCodeRequest request) {
        try {
            String resetToken = authService.verifyCode(request.getEmail(), request.getCode());
            // 인증 성공 시 리셋 토큰 반환
            return ResponseEntity.ok(Map.of(
                "message", "인증되었습니다.",
                "resetToken", resetToken
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 3. 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody AuthDto.PasswordResetRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "비밀번호가 성공적으로 변경되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "비밀번호 변경 중 오류가 발생했습니다."));
        }
    }
	
}
