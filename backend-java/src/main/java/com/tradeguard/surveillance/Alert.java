package com.tradeguard.surveillance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "surveillance_alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_key", nullable = false, unique = true, length = 120)
    private String alertKey;

    @Column(name = "rule_code", nullable = false, length = 32)
    private String ruleCode;

    @Column(nullable = false, length = 16)
    private String symbol;

    @Column(nullable = false, length = 8)
    private String severity;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected Alert() {
    }

    public Alert(String alertKey, String ruleCode, String symbol, String severity,
                 Long tradeId, String description, Instant occurredAt) {
        this.alertKey = alertKey;
        this.ruleCode = ruleCode;
        this.symbol = symbol;
        this.severity = severity;
        this.tradeId = tradeId;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSeverity() {
        return severity;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public String getDescription() {
        return description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
