package com.tradeguard.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "帳號不可為空")
        @Size(min = 3, max = 50, message = "帳號長度需為 3 至 50 個字元")
        @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "帳號僅接受英數字、點、底線與連字號")
        String username,

        @NotBlank(message = "密碼不可為空")
        @Size(min = 12, max = 72, message = "密碼長度需為 12 至 72 個字元")
        String password
) {
}
