package com.tradeguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "tradeguard.auth")
public class AuthProperties {

    private String jwtSecret = "";
    private Duration tokenLifetime = Duration.ofMinutes(15);
    private String cookieName = "tradeguard_session";
    private boolean cookieSecure;
    private List<String> allowedOrigins = new ArrayList<>();
    private int loginFailureThreshold = 5;
    private Duration loginLockDuration = Duration.ofMinutes(15);

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getTokenLifetime() {
        return tokenLifetime;
    }

    public void setTokenLifetime(Duration tokenLifetime) {
        this.tokenLifetime = tokenLifetime;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public boolean isCookieSecure() {
        return cookieSecure;
    }

    public void setCookieSecure(boolean cookieSecure) {
        this.cookieSecure = cookieSecure;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public int getLoginFailureThreshold() {
        return loginFailureThreshold;
    }

    public void setLoginFailureThreshold(int loginFailureThreshold) {
        this.loginFailureThreshold = loginFailureThreshold;
    }

    public Duration getLoginLockDuration() {
        return loginLockDuration;
    }

    public void setLoginLockDuration(Duration loginLockDuration) {
        this.loginLockDuration = loginLockDuration;
    }

    @PostConstruct
    void validateConfiguration() {
        if (tokenLifetime == null || tokenLifetime.compareTo(Duration.ofSeconds(1)) < 0
                || tokenLifetime.compareTo(Duration.ofHours(1)) > 0) {
            throw new IllegalStateException("JWT lifetime must be between 1 second and 1 hour.");
        }
        if (loginFailureThreshold < 1 || loginLockDuration == null
                || loginLockDuration.isZero() || loginLockDuration.isNegative()) {
            throw new IllegalStateException("Login lock settings must be positive.");
        }
        if (cookieName == null || !cookieName.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalStateException("JWT cookie name contains unsupported characters.");
        }
        if (allowedOrigins == null) {
            throw new IllegalStateException("CORS origins must be configured as a list.");
        }
        for (String origin : allowedOrigins) {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getHost().contains("*")
                    || uri.getUserInfo() != null
                    || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalStateException("Configured CORS origins must be origin-only URLs.");
            }
        }
    }
}
