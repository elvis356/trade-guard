package com.tradeguard.security;

import com.tradeguard.config.AuthProperties;
import com.tradeguard.user.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final AuthProperties properties;
    private final SecretKey signingKey;

    public JwtService(AuthProperties properties) {
        this.properties = properties;
        String secret = properties.getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "tradeguard.auth.jwt-secret must contain at least 32 UTF-8 bytes.");
        }
        if (properties.getTokenLifetime().compareTo(java.time.Duration.ofSeconds(1)) < 0
                || properties.getTokenLifetime().compareTo(java.time.Duration.ofHours(1)) > 0) {
            throw new IllegalStateException("JWT lifetime must be between 1 second and 1 hour.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public IssuedToken issue(AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getTokenLifetime());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .id(tokenId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new IssuedToken(token, tokenId, expiresAt);
    }

    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(signingKey).build()
                .parseSignedClaims(token).getPayload();
        String subject = claims.getSubject();
        String role = claims.get("role", String.class);
        String tokenId = claims.getId();
        Date expiration = claims.getExpiration();
        if (subject == null || role == null || tokenId == null || expiration == null) {
            throw new JwtException("JWT is missing required claims.");
        }
        return new TokenClaims(subject, role, tokenId, expiration.toInstant());
    }

    public record IssuedToken(String value, String tokenId, Instant expiresAt) {
    }

    public record TokenClaims(String subject, String role, String tokenId, Instant expiresAt) {
    }
}
