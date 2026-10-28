package com.tradeguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "eod_reports")
public class EodReport {

    @Id
    @Column(name = "business_date")
    private LocalDate businessDate;

    @Column(name = "trade_count", nullable = false)
    private long tradeCount;

    @Column(name = "xml_content", nullable = false, columnDefinition = "TEXT")
    private String xmlContent;

    @Column(name = "content_sha256", nullable = false, length = 64)
    private String contentSha256;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected EodReport() {
    }

    public EodReport(LocalDate businessDate, long tradeCount,
                     String xmlContent, String contentSha256) {
        this.businessDate = businessDate;
        this.tradeCount = tradeCount;
        this.xmlContent = xmlContent;
        this.contentSha256 = contentSha256;
    }

    @PrePersist
    @PreUpdate
    void updateGeneratedAt() {
        generatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public long getTradeCount() {
        return tradeCount;
    }

    public String getXmlContent() {
        return xmlContent;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public void updateContent(long tradeCount, String xmlContent, String contentSha256) {
        this.tradeCount = tradeCount;
        this.xmlContent = xmlContent;
        this.contentSha256 = contentSha256;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }
}
