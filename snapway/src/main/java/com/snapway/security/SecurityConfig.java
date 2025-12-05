package com.snapway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
            // REST API이므로 CSRF 보안 비활성화
            .csrf(csrf -> csrf.disable())
            
            // CORS 설정 (WebConfig에서 설정했더라도 Security에서도 허용해야 함)
            .cors(cors -> cors.configure(http))
            
            // Form Login & Http Basic 비활성화 (순수 REST API 방식)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 회원가입, 로그인, 중복체크 등은 인증 없이 접근 허용
                .requestMatchers("/api/member/regist", "/api/member/login", "/api/member/logout", "/api/member/check-email").permitAll()
                // 정적 리소스 허용 (필요 시)
                .requestMatchers("/css/**", "/images/**", "/js/**").permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            );

        return http.build();
    }
}