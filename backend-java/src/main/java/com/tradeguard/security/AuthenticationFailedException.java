package com.tradeguard.security;

public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException() {
        super("帳號或密碼錯誤。");
    }
}
