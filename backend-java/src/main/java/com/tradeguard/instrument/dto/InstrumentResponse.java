package com.tradeguard.instrument.dto;

import com.tradeguard.instrument.Instrument;

import java.time.Instant;

public record InstrumentResponse(
        Long id,
        String symbol,
        String name,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static InstrumentResponse from(Instrument instrument) {
        return new InstrumentResponse(instrument.getId(), instrument.getSymbol(),
                instrument.getName(), instrument.isActive(),
                instrument.getCreatedAt(), instrument.getUpdatedAt());
    }
}
