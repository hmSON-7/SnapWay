package com.snapway.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class CsrfController {

	// 애플리케이션 실행 시 최초 한 번 xsrf-token발행해서 클라쪽으로 전달
    @PostMapping("/api/csrf")
    public CsrfToken csrf(HttpServletRequest request) {
    	log.debug("XSRF-TOKEN 발급됨");
        return (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    }
}
