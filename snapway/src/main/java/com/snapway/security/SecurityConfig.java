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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	
	@Value("${app.frontServer-origin:http://localhost:5173}")
    private String frontServerOrigin;
    
    @Value("${app.self-origin:http://localhost:8080}")
    private String selfOrigin;

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
    	// csrf 비활성화 할건지 활성화 할건지 상의하고 선택해서 사용
    	
    	http
	    	.csrf(csrf->csrf.disable())
	
	        // CORS
	        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	
	        // 폼로그인/Basic 비활성화
	        .formLogin(form -> form.disable())
	        .httpBasic(basic -> basic.disable())
	
//	        // 권한 설정
//	        .authorizeHttpRequests(auth -> auth
//	            .requestMatchers(
//	                "/api/member/regist",
//	                "/api/member/login",
//	                "/api/member/logout",
//	                "/api/member/check-email",
//	                "/api/csrf"   // csrf 토큰 발급용
//	            ).permitAll()
//	            .anyRequest().authenticated()
//	        );
	        
	        // [핵심] 모든 요청 허용 (로그인 여부 체크는 Controller에서 직접 수행)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() 
            );


        return http.build();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                frontServerOrigin,
                selfOrigin,
                "http://localhost:5173"
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