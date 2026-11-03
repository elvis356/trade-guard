package com.tradeguard.security;

import com.tradeguard.config.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimitFilter.class);
    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";

    private final AuthStateStore authStateStore;
    private final AuthProperties properties;
    private final SecurityErrorWriter errorWriter;

    public AuthRateLimitFilter(AuthStateStore authStateStore, AuthProperties properties,
                              SecurityErrorWriter errorWriter) {
        this.authStateStore = authStateStore;
        this.properties = properties;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !(LOGIN_PATH.equals(request.getRequestURI())
                || REGISTER_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        boolean login = LOGIN_PATH.equals(request.getRequestURI());
        int limit = login ? properties.getLoginRateLimit()
                : properties.getRegistrationRateLimit();
        Duration window = login ? properties.getLoginRateWindow()
                : properties.getRegistrationRateWindow();
        String bucket = (login ? "login:" : "register:")
                + request.getRemoteAddr();

        boolean allowed;
        try {
            allowed = authStateStore.allowRequest(bucket, limit, window);
        } catch (AuthStateUnavailableException ex) {
            log.error("Authentication rate-limit state unavailable for {}",
                    request.getRequestURI(), ex);
            errorWriter.write(response, HttpStatus.SERVICE_UNAVAILABLE,
                    "登入狀態服務暫時無法使用。", request.getRequestURI());
            return;
        }
        if (!allowed) {
            response.setHeader("Retry-After", Long.toString(Math.max(1, window.toSeconds())));
            errorWriter.writeSecurityEvent(request, response, HttpStatus.TOO_MANY_REQUESTS,
                    "請求次數過多，請稍後再試。");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
