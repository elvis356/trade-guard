package com.tradeguard.surveillance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CaseRepository extends JpaRepository<SurveillanceCase, Long> {

    boolean existsByAlertId(Long alertId);

    Optional<SurveillanceCase> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SurveillanceCase c where c.id = :id")
    Optional<SurveillanceCase> findByIdForUpdate(@Param("id") Long id);

    Page<SurveillanceCase> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);
}
