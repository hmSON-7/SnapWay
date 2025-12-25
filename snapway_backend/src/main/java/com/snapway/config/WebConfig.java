package com.snapway.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${spring.servlet.multipart.location}")
    private String basePath;
    
    @Value("${app.frontServer-origin:http://localhost:5173}") 
    private String frontOrigin;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	if (basePath == null || basePath.isBlank()) {
            log.warn("파일 저장 경로(basePath)가 설정되지 않아 이미지 서빙이 불가능합니다.");
            return;
        }
    	
        Path uploadPath = Paths.get(basePath).toAbsolutePath().normalize();
        String location = uploadPath.toUri().toString();
        
        if (!location.endsWith("/")) {
            location += "/";
        }
        
        log.info("이미지 리소스 핸들러 등록: /files/** -> {}", location);

        registry.addResourceHandler("/files/**")
                .addResourceLocations(location)
                .setCachePeriod(3600);
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        log.info("CORS 설정 허용 오리진: {}", frontOrigin);
        
        registry.addMapping("/**") // 모든 API 경로에 대해
                .allowedOrigins(frontOrigin) // 프론트엔드 주소 허용
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 허용할 HTTP 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true) // 쿠키/인증정보 포함 허용
                .maxAge(3600); // Preflight 요청 캐시 시간 (1시간)
    }
}