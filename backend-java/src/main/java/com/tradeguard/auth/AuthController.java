package com.tradeguard.auth;

import com.tradeguard.common.DuplicateResourceException;
import com.tradeguard.config.AuthProperties;
import com.tradeguard.security.AuthService;
import com.tradeguard.security.AuthService.AuthSession;
import com.tradeguard.user.Role;
import com.tradeguard.user.UserService;
import com.tradeguard.user.dto.CreateUserRequest;
import com.tradeguard.user.dto.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, UserService userService,
                          AuthProperties properties) {
        this.authService = authService;
        this.userService = userService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            UserResponse user = userService.create(new CreateUserRequest(
                    request.username(), request.password(), Role.TRADER));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(user);
        } catch (DuplicateResourceException ex) {
            throw new DuplicateResourceException("帳號無法使用，請改用其他帳號。");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthSession session = authService.login(request.username(), request.password());
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), session.token())
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.between(java.time.Instant.now(), session.expiresAt()))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(new LoginResponse(session.user(), session.expiresAt()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = findCookie(request);
        if (token != null && !token.isBlank()) {
            authService.logout(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(new CurrentUserResponse(authentication.getName(),
                Role.valueOf(authority.replaceFirst("^ROLE_", ""))));
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

    private ResponseCookie expiredCookie() {
        return ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/api")
                .maxAge(Duration.ZERO)
                .build();
    }

    public record CurrentUserResponse(String username, Role role) {
    }
}
