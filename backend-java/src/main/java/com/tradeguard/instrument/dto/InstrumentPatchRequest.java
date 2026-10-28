package com.tradeguard.instrument.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InstrumentPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "商品名稱不可為空白")
        @Size(max = 100, message = "商品名稱最多 100 個字元")
        String name,
        Boolean active,
        @jakarta.validation.constraints.DecimalMin(value = "0.01",
                message = "參考收盤價必須大於 0")
        @jakarta.validation.constraints.Digits(integer = 8, fraction = 4,
                message = "參考收盤價最多 8 位整數與 4 位小數")
        BigDecimal referencePrice
) {
}
