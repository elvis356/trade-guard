package com.tradeguard.instrument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    boolean existsBySymbolAndIdNot(String symbol, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Instrument i where i.symbol = :symbol and i.active = true")
    Optional<Instrument> findActiveBySymbolForUpdate(@Param("symbol") String symbol);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Instrument i where i.symbol = :symbol")
    Optional<Instrument> findBySymbolForUpdate(@Param("symbol") String symbol);
}
