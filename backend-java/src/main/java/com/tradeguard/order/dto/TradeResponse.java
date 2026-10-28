package com.tradeguard.order.dto;

import com.tradeguard.order.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TradeResponse(
        Long tradeId,
        String symbol,
        Long buyOrderId,
        Long sellOrderId,
        BigDecimal price,
        long qty,
        Instant createdAt
) {
    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade.getId(), trade.getSymbol(), trade.getBuyOrderId(),
                trade.getSellOrderId(), trade.getPrice(), trade.getQuantity(),
                trade.getCreatedAt());
    }

    public record Page(
            List<TradeResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public static Page from(org.springframework.data.domain.Page<TradeResponse> page) {
            return new Page(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}
