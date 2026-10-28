package com.tradeguard.surveillance;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "surveillance_cases")
public class SurveillanceCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_id", nullable = false, unique = true)
    private Long alertId;

    @Column(name = "proposed_by", nullable = false, length = 64)
    private String proposedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private CaseStatus status;

    @Column(name = "reviewed_by", length = 64)
    private String reviewedBy;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    protected SurveillanceCase() {
    }

    public SurveillanceCase(Long alertId, String proposedBy) {
        this.alertId = alertId;
        this.proposedBy = proposedBy;
        this.status = CaseStatus.PROPOSED;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void review(String reviewer, boolean approved, String note) {
        status = approved ? CaseStatus.APPROVED : CaseStatus.REJECTED;
        reviewedBy = reviewer;
        reviewNote = note;
        reviewedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public String getProposedBy() {
        return proposedBy;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }
}
