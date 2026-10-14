package com.sozureke.ledger.common;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityProblemHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, ErrorCode.UNAUTHENTICATED, request.getRequestURI());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        write(response, ErrorCode.ACCESS_DENIED, request.getRequestURI());
    }

    private void write(HttpServletResponse response, ErrorCode code, String path) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = "{\"type\":\"about:blank\",\"title\":\"" + code.getTitle()
                + "\",\"status\":" + code.getStatus().value()
                + ",\"detail\":\"" + code.getTitle()
                + "\",\"instance\":\"" + path.replace("\"", "")
                + "\",\"code\":\"" + code.name()
                + "\",\"traceId\":\"" + UUID.randomUUID() + "\"}";
        response.getWriter().write(body);
    }
}
