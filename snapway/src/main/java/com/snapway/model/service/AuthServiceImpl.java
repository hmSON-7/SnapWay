package com.snapway.model.service;

import java.sql.SQLException;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snapway.model.dto.AuthDto;
import com.snapway.model.dto.AuthDto.PasswordResetRequest;
import com.snapway.model.mapper.MemberMapper;
import com.snapway.util.RedisUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
	
	private final MemberMapper memberMapper;
	private final EmailService emailService;
	private final RedisUtil redisUtil;
	private final PasswordEncoder pwEncoder;
	
	// 사용자 인증 코드 만료 시간 : 3분(180초)
	private static final long AUTH_CODE_EXPIRATION = 180;
	
	// 비밀번호 재설정 토큰 만료 시간 : 10분(600초)
	private static final long RESET_TOKEN_EXPIRATION = 600;

	/**
	 * 1. 인증 코드 생성 및 이메일 발송
	 */
	@Override
	public void sendVerificationCode(String email) throws SQLException {
		// 1) 가입된 이메일인지 확인
		if(memberMapper.checkEmail(email) == 0) {
			throw new IllegalArgumentException("가입되지 않은 이메일입니다.");
		}
		
		// 2) 인증 코드 생성
		String authCode = emailService.createAuthCode();
		
		// 3) Redis 저장(Key: AuthCode:{email}, Value:{code})
		redisUtil.setDataExpire("AuthCode:" + email, authCode, AUTH_CODE_EXPIRATION);
		
		// 4) 이메일 발송
		emailService.sendEmail(email,  authCode);
	}

	/**
	 * 2. 인증 코드 검증 및 리셋 토큰 발급
	 */
	@Override
	public String verifyCode(String email, String code) {
		String redisCode = redisUtil.getData("AuthCode:" + email);
		if(redisCode == null) {
			throw new IllegalArgumentException("인증 시간이 만료되었거나 요청한 적이 없습니다.");
		}
		
		if(!redisCode.equals(code)) {
			throw new IllegalArgumentException("인증 번호가 일치하지 않습니다.");
		}
		
		// 1) 사용된 인증 코드는 즉시 삭제(재사용 방지)
		redisUtil.deleteData("AuthCode:" + email);
		
		// 2) 비밀번호 변경 권한을 증명하는 '리셋 토큰' 생성
		String resetToken = UUID.randomUUID().toString();
		
		// 3) Redis 저장(Key: ResetToken:{email}, Value:{token})
		redisUtil.setDataExpire("ResetToken:" + email, resetToken, RESET_TOKEN_EXPIRATION);
		
		return resetToken;
	}

	/**
	 * 3. 비밀번호 변경
	 */
	@Override
	@Transactional
	public void resetPassword(AuthDto.PasswordResetRequest request) throws SQLException {
		String email = request.getEmail();
		String resetToken = request.getResetToken();
		String newPw = request.getNewPassword();
		
		// 1) 리셋 토큰 검증
		String redisToken = redisUtil.getData("ResetToken:" + email);
		if(redisToken == null || !redisToken.equals(resetToken)) {
			throw new IllegalArgumentException("유효하지 않거나 만료된 접근입니다. 처음부터 다시 인증해주세요.");
		}
		
		// 2) 비밀번호 암호화
		String encodedPw = pwEncoder.encode(newPw);
		
		// 3) DB 업데이트
		int result = memberMapper.updatePasswordByEmail(email, encodedPw);
		if(result == 0) {
			throw new RuntimeException("비밀번호 변경 실패: 회원 정보를 찾을 수 없음");
		}
		
		// 4) 사용된 리셋 토큰 삭제
		redisUtil.deleteData("ResetToken:" + email);
		
		log.info("비밀번호 변경 완료: {}", email);
	}
	
}
