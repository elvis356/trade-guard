package com.tradeguard.audit;

import java.time.Instant;
import java.util.List;

public record ReconciliationReport(boolean balanced, long ordersChecked,
                                   long tradesChecked, long eventsChecked,
                                   List<ReconciliationIssue> issues,
                                   Instant reconciledAt) {
}
