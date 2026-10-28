package com.tradeguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "eod_runs")
public class EodRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "orders_checked", nullable = false)
    private long ordersChecked;

    @Column(name = "trades_checked", nullable = false)
    private long tradesChecked;

    @Column(name = "events_checked", nullable = false)
    private long eventsChecked;

    @Column(name = "issues_json", nullable = false, columnDefinition = "TEXT")
    private String issuesJson;

    @Column(name = "report_sha256", length = 64)
    private String reportSha256;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    protected EodRun() {
    }

    public EodRun(LocalDate businessDate, boolean completed,
                  ReconciliationReport reconciliation, String issuesJson,
                  String reportSha256) {
        this.businessDate = businessDate;
        this.completed = completed;
        this.ordersChecked = reconciliation.ordersChecked();
        this.tradesChecked = reconciliation.tradesChecked();
        this.eventsChecked = reconciliation.eventsChecked();
        this.issuesJson = issuesJson;
        this.reportSha256 = reportSha256;
    }

    @PrePersist
    void onCreate() {
        generatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }
}
