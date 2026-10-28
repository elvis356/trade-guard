package com.tradeguard.security;

import com.tradeguard.config.AuthProperties;
import com.tradeguard.user.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final AuthProperties properties;
    private final JwtService jwtService;
    private final AuthStateStore authStateStore;
    private final SecurityErrorWriter errorWriter;

    public JwtCookieAuthenticationFilter(AuthProperties properties, JwtService jwtService,
                                         AuthStateStore authStateStore,
                                         SecurityErrorWriter errorWriter) {
        this.properties = properties;
        this.jwtService = jwtService;
        this.authStateStore = authStateStore;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = findCookie(request);
        if (token != null) {
            try {
                JwtService.TokenClaims claims = jwtService.parse(token);
                if (!authStateStore.isTokenRevoked(claims.tokenId())) {
                    Role role = Role.valueOf(claims.role());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.subject(), null,
                            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            } catch (AuthStateUnavailableException ex) {
                errorWriter.write(response, HttpStatus.SERVICE_UNAVAILABLE,
                        "登入狀態服務暫時無法使用。", request.getRequestURI());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String findCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

}
