package com.tradeguard.audit;

import org.springframework.data.repository.Repository;

import java.util.List;

public interface AuditEntryRepository extends Repository<AuditEntry, Long> {

    AuditEntry saveAndFlush(AuditEntry entry);

    List<AuditEntry> findAllByOrderByIdAsc();
}
