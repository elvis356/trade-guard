package com.tradeguard.instrument.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InstrumentPatchRequest(
        @Pattern(regexp = ".*\\S.*", message = "商品名稱不可為空白")
        @Size(max = 100, message = "商品名稱最多 100 個字元")
        String name,
        Boolean active
) {
}
