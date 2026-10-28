package com.tradeguard.audit;

import java.time.Instant;
import java.time.LocalDate;

public record EodRunResult(long runId, boolean completed, LocalDate businessDate,
                           long tradeCount, ReconciliationReport reconciliation,
                           String reportSha256, Instant generatedAt) {
}
