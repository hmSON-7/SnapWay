package com.snapway.controller;

import java.util.*;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

/**
 * [보관용] CSRF 토큰 발급용 컨트롤러
 * 현재 SecurityConfig에서 csrf.disable() 설정되었으므로 사용하지 않음.
 * 추후 보안 강화 시 재사용 가능.
 */
@Deprecated
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CsrfController {

    @PostMapping("/csrf")
    public Map<String, Object> csrf(CsrfToken token, HttpSession session) {
        // 🔴 세션 강제 생성
        session.setAttribute("CSRF_INIT", "true");

        Map<String, Object> body = new HashMap<>();
        body.put("parameterName", token.getParameterName());
        body.put("headerName", token.getHeaderName());
        body.put("token", token.getToken());
        return body;
    }
}

