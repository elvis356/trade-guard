package com.tradeguard.order.dto;

import com.tradeguard.order.OrderSide;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
        @JsonProperty("client_order_id")
        @JsonAlias("clientOrderId")
        @NotNull(message = "client_order_id 不可為空")
        UUID clientOrderId,
        @NotNull(message = "商品代碼不可為空")
        @Pattern(regexp = "[A-Z0-9._-]{1,16}", message = "商品代碼格式錯誤")
        String symbol,
        @NotNull(message = "買賣方向不可為空")
        OrderSide side,
        @NotNull(message = "委託價格不可為空")
        @DecimalMin(value = "0.01", message = "委託價格必須大於 0")
        @Digits(integer = 8, fraction = 4, message = "委託價格最多 8 位整數與 4 位小數")
        BigDecimal price,
        @Positive(message = "委託數量必須大於 0")
        @jakarta.validation.constraints.Max(value = 1000000000L, message = "委託數量超出上限")
        long qty
) {
}
