package com.tradeguard.instrument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findBySymbol(String symbol);

    boolean existsBySymbol(String symbol);

    boolean existsBySymbolAndIdNot(String symbol, Long id);
}
