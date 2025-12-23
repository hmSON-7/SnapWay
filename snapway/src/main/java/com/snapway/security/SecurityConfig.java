package com.snapway.security;

import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Value("${app.frontServer-origin}")
    private String frontServerOrigin;
	@Value("${app.self-origin}")
    private String selfOrigin;
	
	private final JwtUtil jwtUtil;

	private final JwtAuthenticationFilter jwtFilter;
	public SecurityConfig(JwtUtil jwtUtil, JwtAuthenticationFilter jwtFilter) {
		this.jwtFilter = jwtFilter;
		this.jwtUtil = jwtUtil;
	}
    // 1. 비밀번호 암호화 빈 등록 (BCrypt)
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. AuthenticationManager 빈 등록 (로그인 로직에서 사용)
    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // 3. Security Filter Chain 설정
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    	http
    	.csrf(csrf->csrf.disable())

        // CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // 폼로그인/Basic 비활성화
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())

        // 권한 설정
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(
                "/api/member/regist",
                "/api/member/login",
                "/api/member/logout",
                "/api/member/check-email",
                "/error/**",
                "/api/member/fetchMyInfo",
                "/api/csrf",   // csrf 토큰 발급용
                "/api/article/**",
                // 테스트를 위한 임시 개방
                "/api/trip/**"
            ).permitAll()
            .anyRequest().authenticated()
        )
        
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                frontServerOrigin,
                selfOrigin
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        // 필요하면 노출 헤더 추가
        // 일단은 애플리케이션 실행 시 자동 한 번 발급으로 설정함.
        // configuration.setExposedHeaders(List.of("X-XSRF-TOKEN"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}