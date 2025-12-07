package com.snapway.controller;

import java.util.*;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

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

