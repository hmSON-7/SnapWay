package com.snapway.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private JwtUtil jwtUtil;

	public JwtAuthenticationFilter(JwtUtil ju) {
		this.jwtUtil = ju;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		log.debug("====== JWT 필터 실행 시작 ======");
		log.debug("요청 URI: {}", request.getRequestURI());
		log.debug("요청 메서드: {}", request.getMethod());
		String authHeader = request.getHeader("Authorization");
		log.debug("Authorization 헤더: {}", authHeader);

		if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			log.debug("추출된 토큰: {}", token);

			// 유효한 토근이면서 accessToken일 경우만 인증 처리
			if (jwtUtil.isTokenValid(token) && jwtUtil.isAccessToken(token)) {
				log.debug("토큰 검증 성공!");
				String userName = jwtUtil.getUserName(token);
				int userId = (int) jwtUtil.parseClaims(token).get("userId");
				String realName = (String) jwtUtil.parseClaims(token).get("realName");
				log.debug("토큰에서 추출한 사용자ID: {}", userId);
				log.debug("토큰에서 추출한 사용자명: {}", realName);

				List<String> roles = jwtUtil.getRoles(token);
				List<GrantedAuthority> authorities = new ArrayList<>();
				for (String role : roles) {
					authorities.add(new SimpleGrantedAuthority(role));
				}


				// principal, credential, 권한 목록이 들어간다.
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userName, null, authorities);
				authentication.setDetails(Map.of("userId", userId, "realName", realName));

				SecurityContextHolder.getContext().setAuthentication(authentication);
				log.debug("SecurityContext에 인증 정보 설정 완료");
			}
		}
		filterChain.doFilter(request, response);
	}
}
