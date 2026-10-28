package com.tradeguard.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    Page<Trade> findBySymbolOrderByIdDesc(String symbol, Pageable pageable);

    Optional<Trade> findFirstBySymbolOrderByIdDesc(String symbol);

    List<Trade> findAllByOrderByIdAsc();

    List<Trade> findAllByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            Instant start, Instant end);
}
