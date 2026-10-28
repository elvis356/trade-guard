package com.tradeguard.market;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketQuote(String symbol, String name, BigDecimal referencePrice,
                          BigDecimal lastPrice, long lastQuantity, Instant updatedAt) {
}
