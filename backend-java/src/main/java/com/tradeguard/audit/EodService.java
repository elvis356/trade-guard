package com.tradeguard.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeguard.config.MarketProperties;
import com.tradeguard.order.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;

@Service
public class EodService {

    private final ReconciliationService reconciliationService;
    private final TradeRepository trades;
    private final DailyReportService reports;
    private final AuditService audit;
    private final MarketProperties market;
    private final EodRunRepository runs;
    private final ObjectMapper objectMapper;

    public EodService(ReconciliationService reconciliationService, TradeRepository trades,
                      DailyReportService reports, AuditService audit, MarketProperties market,
                      EodRunRepository runs, ObjectMapper objectMapper) {
        this.reconciliationService = reconciliationService;
        this.trades = trades;
        this.reports = reports;
        this.audit = audit;
        this.market = market;
        this.runs = runs;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EodRunResult run(LocalDate requestedDate) {
        LocalDate businessDate = requestedDate == null
                ? LocalDate.now(market.getZone()) : requestedDate;
        ReconciliationReport reconciliation = reconciliationService.reconcile();
        long tradeCount = trades.findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
                businessDate.atStartOfDay(market.getZone()).toInstant(),
                businessDate.plusDays(1).atStartOfDay(market.getZone()).toInstant()).size();
        boolean completed = reconciliation.balanced();
        EodReport report = completed ? reports.generateAndStore(businessDate) : null;
        EodRun run = runs.saveAndFlush(new EodRun(businessDate, completed, reconciliation,
                serializeIssues(reconciliation.issues()),
                report == null ? null : report.getContentSha256()));
        audit.record(null, "system:eod", completed ? "EOD_COMPLETED" : "EOD_FAILED",
                "date=" + businessDate + ",trades=" + tradeCount
                        + ",issues=" + reconciliation.issues().size());
        return new EodRunResult(run.getId(), completed, businessDate, tradeCount,
                reconciliation, report == null ? null : report.getContentSha256(),
                Instant.now());
    }

    private String serializeIssues(java.util.List<ReconciliationIssue> issues) {
        try {
            return objectMapper.writeValueAsString(issues);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize EOD reconciliation issues.", ex);
        }
    }
}
