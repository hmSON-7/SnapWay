package com.snapway.security;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	private final SecretKey secretKey;
	private final long accessTokenExpire;
	private final long refreshTokenExpire;
	
	public JwtUtil(
			@Value("${app.jwt.secretkey}") String key,
			@Value("${app.jwt.access-token-expiretime}") long access, 
			@Value("${app.jwt.refresh-token-expiretime}") long refresh
	) {
		this.secretKey = Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8));
		this.accessTokenExpire = access;
		this.refreshTokenExpire = refresh;
	}
	
	// 엑세스 토큰 생성 메소드
	public String generateAccessToken(int userId, String email, List<String> roles) {
//		LocalDateTime now = LocalDateTime.now();
//		LocalDateTime expire = now.plusSeconds(accessTokenExpire);
		Date now = new Date();
		Date expire = new Date(now.getTime() + accessTokenExpire);
		
		return Jwts.builder()
				.subject(email)
				.claim("userId", userId)
				.claim("roles", roles)
				.claim("type", "access")
				.issuedAt(now)
				.expiration(expire)
				.signWith(secretKey)
				.compact();
	}
	
	// 리프레시 토큰 생성 메소드
	public String generateRefreshToken(String username) {
	    Date now = new Date();
	    Date expire = new Date(now.getTime() + refreshTokenExpire);

	    return Jwts.builder()
	            .subject(username)
	            .claim("type", "refresh")
	            .issuedAt(now)
	            .expiration(expire)
	            .signWith(secretKey)
	            .compact();
	}

	
	// 토큰 파싱 메소드
	public Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch(ExpiredJwtException e) {
			return e.getClaims(); // 놀랍게도 만료된 토큰도 꺼내 쓸 수 있게 get이 지원된다.
		}
	}
	
	// 토큰 만료 검증 메소드
	// 만료 됬을 경우에 true를 반환
	public boolean isTokenExpired(String token) {
		Date expiration = parseClaims(token).getExpiration();
		return expiration.before(new Date());
	}
	
	// 토큰 전체 유효성 검사 (서명 + 구조 + 만료)
	public boolean isTokenValid(String token) {
	    try {
	        Claims claims = Jwts.parser()
	                .verifyWith(secretKey) // 서명 검증 설정
	                .build()
	                .parseSignedClaims(token)
	                .getPayload();

	        Date expiration = claims.getExpiration();
	        return expiration != null && expiration.after(new Date());
	    } catch (ExpiredJwtException e) {
	        // 만료된 토큰은 유효하지 않다고 처리
	        return false;
	    } catch (Exception e) {
	        // 서명 불일치, 형식 오류 등 모두 false
	        return false;
	    }
	}

	
	// 토큰의 종류를 확인하는 메소드
	public boolean isAccessToken(String token) {
		String type = (String)parseClaims(token).get("type");
		return "access".equals(type);
	}
	public boolean isRefreshToken(String token) {
		String type = (String)parseClaims(token).get("type");
		return "refresh".equals(type);
	}
	
	// 이름을 추출하는 메소드
	public String getUserName(String token) {
		return (String) parseClaims(token).getSubject();
	}
	
	// 권한을 추출하는 메소드
	public List<String> getRoles(String token) {
		List<String> roles = (List<String>)parseClaims(token).get("roles");
		return roles != null ? roles : List.of(); 
	}
	
}
