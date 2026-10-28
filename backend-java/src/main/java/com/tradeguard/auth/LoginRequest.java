package com.tradeguard.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "帳號不可為空")
        @Size(max = 50, message = "帳號不可超過 50 個字元")
        String username,
        @NotBlank(message = "密碼不可為空")
        @Size(max = 72, message = "密碼不可超過 72 個字元")
        String password
) {
}
