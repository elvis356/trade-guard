package com.tradeguard.security;

public class LoginLockedException extends RuntimeException {

    public LoginLockedException() {
        super("登入失敗次數過多，請稍後再試。");
    }
}
