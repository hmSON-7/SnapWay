package com.snapway.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
	 private JwtUtil jwtUtil;
	 
	 public JwtAuthenticationFilter(JwtUtil ju) {
		 this.jwtUtil = ju;
	 }

	 @Override
	 protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String authHeader = request.getHeader("Authorization");
		
		if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
			String token = authHeader.substring(7);
			
			// 만료되지 않았으면서 accessToken일 경우만 인증 처리
			if(!jwtUtil.isTokenExpired(token) && jwtUtil.isAccessToken(token)) {
				String userName = jwtUtil.getUserName(token);
				List<String> roles = jwtUtil.getRoles(token);
				List<GrantedAuthority> authorities = new ArrayList<>();
				for(String role : roles) {
					authorities.add(new SimpleGrantedAuthority(role));
				}
				
				// principal, credential, 권한 목록이 들어간다.
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userName, null, authorities
				);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
		}
		filterChain.doFilter(request, response);
	 }
}
