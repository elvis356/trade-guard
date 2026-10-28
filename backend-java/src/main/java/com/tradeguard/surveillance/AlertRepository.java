package com.tradeguard.surveillance;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    boolean existsByAlertKey(String alertKey);

    Page<Alert> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);

    Optional<Alert> findById(Long id);
}
