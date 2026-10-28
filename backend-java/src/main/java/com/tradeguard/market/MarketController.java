package com.tradeguard.market;

import com.tradeguard.instrument.InstrumentService;
import com.tradeguard.instrument.dto.InstrumentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final InstrumentService instrumentService;

    public MarketController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping("/{symbol}/info")
    public ResponseEntity<InstrumentResponse> getInstrumentInfo(@PathVariable String symbol) {
        return ResponseEntity.ok(instrumentService.findBySymbol(symbol));
    }
}
