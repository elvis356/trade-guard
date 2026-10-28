package com.tradeguard.security;

import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.config.AuthProperties;
import com.tradeguard.user.AppUser;
import com.tradeguard.user.AppUserRepository;
import com.tradeguard.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthStateStore authStateStore;
    private final AuthProperties properties;
    private final String dummyPasswordHash;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthStateStore authStateStore,
                       AuthProperties properties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authStateStore = authStateStore;
        this.properties = properties;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public AuthSession login(String username, String password) {
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("密碼 UTF-8 編碼長度不可超過 72 bytes。");
        }
        if (authStateStore.isLoginLocked(normalizedUsername,
                properties.getLoginFailureThreshold())) {
            throw new LoginLockedException();
        }

        AppUser user = userRepository.findByUsername(normalizedUsername).orElse(null);
        String hash = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(password, hash);
        if (user == null || !user.isEnabled() || !passwordMatches) {
            if (authStateStore.recordLoginFailure(normalizedUsername,
                    properties.getLoginFailureThreshold(), properties.getLoginLockDuration())) {
                throw new LoginLockedException();
            }
            throw new AuthenticationFailedException();
        }

        authStateStore.clearLoginFailures(normalizedUsername);
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthSession(token.value(), token.expiresAt(), UserResponse.from(user));
    }

    public void logout(String token) {
        try {
            JwtService.TokenClaims claims = jwtService.parse(token);
            Duration remaining = Duration.between(java.time.Instant.now(), claims.expiresAt());
            authStateStore.revokeToken(claims.tokenId(), remaining);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
            // Expired or malformed tokens cannot be used and need no revocation entry.
        }
    }

    public record AuthSession(String token, java.time.Instant expiresAt, UserResponse user) {
    }
}
