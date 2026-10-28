package com.tradeguard.security;

import com.tradeguard.config.AuthProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class OriginValidationFilter extends OncePerRequestFilter {

    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final Set<String> allowedOrigins;
    private final SecurityErrorWriter errorWriter;

    public OriginValidationFilter(AuthProperties properties, SecurityErrorWriter errorWriter) {
        this.allowedOrigins = Set.copyOf(properties.getAllowedOrigins());
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (request.getRequestURI().startsWith("/api/")
                && UNSAFE_METHODS.contains(request.getMethod())
                && origin != null && !allowedOrigins.contains(origin)) {
            errorWriter.write(response, HttpStatus.FORBIDDEN,
                    "請求來源不在允許清單中。", request.getRequestURI());
            return;
        }
        filterChain.doFilter(request, response);
    }
}
