package com.tradeguard.order.dto;

import com.tradeguard.order.TradingOrder;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        @JsonProperty("order_id") Long orderId,
        @JsonProperty("client_order_id") UUID clientOrderId,
        @JsonProperty("seq_no") long seqNo,
        String symbol,
        String side,
        BigDecimal price,
        long qty,
        @JsonProperty("filled_qty") long filledQty,
        @JsonProperty("remaining_qty") long remainingQty,
        String status,
        Instant createdAt
) {
    public static OrderResponse from(TradingOrder order) {
        return new OrderResponse(order.getId(), order.getClientOrderId(), order.getSeqNo(),
                order.getSymbol(), order.getSide().code(), order.getLimitPrice(),
                order.getOriginalQuantity(), order.getFilledQuantity(),
                order.getRemainingQuantity(), order.getStatus().name(), order.getCreatedAt());
    }
}
