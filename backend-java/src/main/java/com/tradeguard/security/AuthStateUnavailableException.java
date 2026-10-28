package com.tradeguard.security;

public class AuthStateUnavailableException extends RuntimeException {

    public AuthStateUnavailableException(String message) {
        super(message);
    }

    public AuthStateUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
