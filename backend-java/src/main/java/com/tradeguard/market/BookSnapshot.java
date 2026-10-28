package com.tradeguard.market;

import java.time.Instant;
import java.util.List;

public record BookSnapshot(String symbol, List<BookLevel> bids, List<BookLevel> asks,
                           Instant updatedAt) {
}
