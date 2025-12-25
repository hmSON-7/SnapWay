package com.snapway.model.service;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.snapway.model.dto.AuthDto;
import com.snapway.model.dto.AuthDto.PasswordResetRequest;
import com.snapway.model.dto.AuthDto.ReissueRequest;
import com.snapway.model.dto.AuthDto.TokenResponse;
import com.snapway.model.dto.Member;
import com.snapway.model.mapper.MemberMapper;
import com.snapway.security.JwtUtil;
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
	private final JwtUtil jwtUtil;
	
	// 사용자 인증 코드 만료 시간 : 3분(180초)
	private static final long AUTH_CODE_EXPIRATION = 180;
	
	// 비밀번호 재설정 토큰 만료 시간 : 10분(600초)
	private static final long RESET_TOKEN_EXPIRATION = 600;
	
	// 리프레시 토큰 만료 시간 (application.properties에서 주입, 단위: 밀리초)
    @Value("${app.jwt.refresh-token-expiretime}")
    private long refreshTokenExpireMillis;

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

	// 4. 리프레시 토큰 Redis 저장(로그인 성공 시 호출) - Key: RT:{email}
	@Override
	public void saveRefreshToken(String email, String refreshToken) {
		// RedisUtil은 초 단위를 사용하므로 밀리초 / 1000 사용
		redisUtil.setDataExpire("RT:" + email, refreshToken, refreshTokenExpireMillis / 1000);
	}

	// 5. 토큰 재발급(Reissue)
	@Override
	public TokenResponse reissue(AuthDto.ReissueRequest request) throws SQLException{
		String refreshToken = request.getRefreshToken();
		
		// 1) 토큰 유효성 및 타입(Refresh) 검증
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        // 2) 토큰에서 유저 정보(Email) 추출
        String email = jwtUtil.getUserName(refreshToken);

        // 3) Redis에 저장된 토큰과 비교
        String redisToken = redisUtil.getData("RT:" + email);
        if (redisToken == null) {
            throw new IllegalArgumentException("만료된 인증 정보입니다. 다시 로그인해주세요.");
        }
        if (!redisToken.equals(refreshToken)) {
            // Redis 값과 다르면 탈취 가능성 있음 -> 저장된 토큰 삭제 (강제 로그아웃)
            redisUtil.deleteData("RT:" + email);
            throw new IllegalArgumentException("토큰 정보가 일치하지 않습니다.");
        }
        
        // 4) 유저 정보(userId, Roles) 조회 (DB 조회 필요)
        Member member = memberMapper.findByEmail(email);
        int userId = member.getId();
        List<String> roles = List.of(member.getRole().name());
		
        // 5) 새 토큰 생성 (Access + Refresh) -> RTR(Refresh Token Rotation) 방식 적용
        String newAccessToken = jwtUtil.generateAccessToken(userId, email, roles);
        String newRefreshToken = jwtUtil.generateRefreshToken(email); // userId 포함 여부는 JwtUtil 구현 확인

        // 6) Redis 업데이트
        saveRefreshToken(email, newRefreshToken);

        return AuthDto.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
	}

	// 6. 로그아웃(Redis 토큰 삭제)
	@Override
	public void logout(String email) {
		redisUtil.deleteData("RT:" + email);
	}
	
}
