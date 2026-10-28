package com.tradeguard.security;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Component
public class RedisAuthStateStore implements AuthStateStore {

    private static final String LOGIN_PREFIX = "tradeguard:login:fail:";
    private static final String REVOKED_PREFIX = "tradeguard:jwt:deny:";
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return count;",
            Long.class);

    private final StringRedisTemplate redis;

    public RedisAuthStateStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public boolean isLoginLocked(String username, int threshold) {
        try {
            String value = redis.opsForValue().get(loginKey(username));
            return value != null && Integer.parseInt(value) >= threshold;
        } catch (DataAccessException | NumberFormatException ex) {
            throw new AuthStateUnavailableException("無法讀取登入鎖定狀態。", ex);
        }
    }

    @Override
    public boolean recordLoginFailure(String username, int threshold, Duration lockDuration) {
        try {
            String key = loginKey(username);
            long expirySeconds = Math.max(1, lockDuration.toSeconds());
            Long count = redis.execute(INCREMENT_WITH_EXPIRY,
                    java.util.List.of(key), Long.toString(expirySeconds));
            if (count == null) {
                throw new AuthStateUnavailableException("無法更新登入失敗計數。");
            }
            return count >= threshold;
        } catch (DataAccessException ex) {
            throw new AuthStateUnavailableException("無法更新登入失敗計數。", ex);
        }
    }

    @Override
    public void clearLoginFailures(String username) {
        try {
            redis.delete(loginKey(username));
        } catch (DataAccessException ex) {
            throw new AuthStateUnavailableException("無法清除登入失敗計數。", ex);
        }
    }

    @Override
    public boolean isTokenRevoked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(REVOKED_PREFIX + tokenId));
        } catch (DataAccessException ex) {
            throw new AuthStateUnavailableException("無法驗證登入狀態。", ex);
        }
    }

    @Override
    public void revokeToken(String tokenId, Duration remainingLifetime) {
        if (remainingLifetime.isZero() || remainingLifetime.isNegative()) {
            return;
        }
        try {
            redis.opsForValue().set(REVOKED_PREFIX + tokenId, "1", remainingLifetime);
        } catch (DataAccessException ex) {
            throw new AuthStateUnavailableException("無法撤銷登入憑證。", ex);
        }
    }

    private String loginKey(String username) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(username.getBytes(StandardCharsets.UTF_8));
            return LOGIN_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime.", ex);
        }
    }
}
