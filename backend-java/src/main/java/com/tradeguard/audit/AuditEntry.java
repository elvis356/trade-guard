package com.tradeguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_entries")
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(nullable = false, length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "row_hash", nullable = false, unique = true, length = 64)
    private String rowHash;

    protected AuditEntry() {
    }

    public AuditEntry(Long userId, String actor, String action, String detail,
                      Instant createdAt, String previousHash, String rowHash) {
        this.userId = userId;
        this.actor = actor;
        this.action = action;
        this.detail = detail;
        this.createdAt = createdAt;
        this.previousHash = previousHash;
        this.rowHash = rowHash;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getActor() {
        return actor;
    }

    public String getAction() {
        return action;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public String getRowHash() {
        return rowHash;
    }
}
