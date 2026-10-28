package com.tradeguard.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface EodReportRepository extends JpaRepository<EodReport, LocalDate> {
}
