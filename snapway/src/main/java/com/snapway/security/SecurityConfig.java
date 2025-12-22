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
//        http
//            // REST API이므로 CSRF 보안 비활성화
//            .csrf(csrf -> csrf.disable())
//            
//            // CORS 설정 (WebConfig에서 설정했더라도 Security에서도 허용해야 함)
//            .cors(cors -> cors.configure(http))
//            
//            // Form Login & Http Basic 비활성화 (순수 REST API 방식)
//            .formLogin(form -> form.disable())
//            .httpBasic(basic -> basic.disable())
//
//            // 요청 권한 설정
//            .authorizeHttpRequests(auth -> auth
//                // 회원가입, 로그인, 중복체크 등은 인증 없이 접근 허용
//                .requestMatchers("/api/member/regist", "/api/member/login", "/api/member/logout", "/api/member/check-email").permitAll()
//                // 정적 리소스 허용 (필요 시)
//                .requestMatchers("/css/**", "/images/**", "/js/**").permitAll()
//                // 그 외 모든 요청은 인증 필요
//                .anyRequest().authenticated()
//            );
    	
    	http
    	
        // CSRF: 쿠키(XSRF-TOKEN) + 헤더(X-XSRF-TOKEN) 조합 사용
//        .csrf(csrf -> csrf
//            .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//            .ignoringRequestMatchers("/api/csrf", "/api/member/login") // 토큰 발급 엔드포인트는 예외
//            
//        )
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
                "/api/csrf"
            ).permitAll()
            .anyRequest().authenticated()
        );


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