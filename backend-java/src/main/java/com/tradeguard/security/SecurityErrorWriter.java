package com.tradeguard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeguard.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityErrorWriter {

    private static final Logger log = LoggerFactory.getLogger(SecurityErrorWriter.class);
    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> writeSecurityEvent(request, response,
                HttpStatus.UNAUTHORIZED, "需要登入才能使用此功能。");
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> writeSecurityEvent(request, response,
                HttpStatus.FORBIDDEN, "目前角色沒有執行此操作的權限。");
    }

    public void writeSecurityEvent(HttpServletRequest request, HttpServletResponse response,
                                   HttpStatus status, String message) throws IOException {
        log.warn("Security request rejected status={} method={} path={} remoteAddr={}",
                status.value(), request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr());
        write(response, status, message, request.getRequestURI());
    }

    public void write(HttpServletResponse response, HttpStatus status,
                      String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                ApiError.of(status.value(), status.getReasonPhrase(), message, path));
    }
}
