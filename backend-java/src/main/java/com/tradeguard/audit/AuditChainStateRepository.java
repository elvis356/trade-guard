package com.tradeguard.audit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuditChainStateRepository extends JpaRepository<AuditChainState, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AuditChainState s where s.id = :id")
    Optional<AuditChainState> findByIdForUpdate(@Param("id") Long id);
}
