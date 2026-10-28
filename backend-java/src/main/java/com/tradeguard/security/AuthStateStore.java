package com.tradeguard.security;

import java.time.Duration;

public interface AuthStateStore {

    boolean isLoginLocked(String username, int threshold);

    boolean recordLoginFailure(String username, int threshold, Duration lockDuration);

    void clearLoginFailures(String username);

    boolean isTokenRevoked(String tokenId);

    void revokeToken(String tokenId, Duration remainingLifetime);
}
