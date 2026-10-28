package com.tradeguard.instrument.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstrumentRequest(
        @NotBlank(message = "商品代碼不可為空")
        @Pattern(regexp = "[A-Z0-9._-]+", message = "商品代碼僅接受大寫英數字、點、底線與連字號")
        @Size(max = 16, message = "商品代碼最多 16 個字元")
        String symbol,

        @NotBlank(message = "商品名稱不可為空")
        @Size(max = 100, message = "商品名稱最多 100 個字元")
        String name,

        boolean active
) {
}
