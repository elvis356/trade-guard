package com.tradeguard.surveillance;

import java.time.Instant;

public record AlertResponse(long id, String ruleCode, String symbol, String severity,
                            long tradeId, String description, Instant occurredAt) {

    public static AlertResponse from(Alert alert) {
        return new AlertResponse(alert.getId(), alert.getRuleCode(), alert.getSymbol(),
                alert.getSeverity(), alert.getTradeId(), alert.getDescription(),
                alert.getOccurredAt());
    }
}
