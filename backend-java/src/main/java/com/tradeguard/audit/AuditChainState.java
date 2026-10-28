package com.tradeguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_chain_state")
public class AuditChainState {

    @Id
    private Long id;

    @Column(name = "last_hash", nullable = false, length = 64)
    private String lastHash;

    protected AuditChainState() {
    }

    public Long getId() {
        return id;
    }

    public String getLastHash() {
        return lastHash;
    }

    public void setLastHash(String lastHash) {
        this.lastHash = lastHash;
    }
}
