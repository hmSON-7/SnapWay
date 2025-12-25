package com.snapway.model.service;

import java.sql.SQLException;

import com.snapway.model.dto.AuthDto;

public interface AuthService {
	
	// 1. 이메일 인증 코드 발송
	void sendVerificationCode(String email) throws SQLException;
	
	// 2. 인증 코드 검증
	String verifyCode(String email, String code);
	
	// 3. 비밀번호 재설정
	void resetPassword(AuthDto.PasswordResetRequest request) throws SQLException;
	
	// 4. 리프레시 토큰 저장(로그인 시 호출)
    void saveRefreshToken(String email, String refreshToken);

    // 5. 토큰 재발급
    AuthDto.TokenResponse reissue(AuthDto.ReissueRequest request) throws SQLException;

    // 6. 로그아웃(Redis 토큰 삭제)
    void logout(String email);
	
}
