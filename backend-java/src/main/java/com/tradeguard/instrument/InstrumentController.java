package com.tradeguard.instrument;

import com.tradeguard.instrument.dto.InstrumentPatchRequest;
import com.tradeguard.instrument.dto.InstrumentRequest;
import com.tradeguard.instrument.dto.InstrumentResponse;
import com.tradeguard.instrument.dto.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/market/instruments")
public class InstrumentController {

    private final InstrumentService service;

    public InstrumentController(InstrumentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<InstrumentResponse> create(@Valid @RequestBody InstrumentRequest request) {
        InstrumentResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/market/instruments/{symbol}")
                .buildAndExpand(created.symbol())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<PageResponse<InstrumentResponse>> findAll(
            @PageableDefault(size = 10, sort = "symbol") Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(service.findAll(pageable)));
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> findBySymbol(@PathVariable String symbol) {
        return ResponseEntity.ok(service.findBySymbol(symbol));
    }

    @PutMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> update(
            @PathVariable String symbol, @Valid @RequestBody InstrumentRequest request) {
        return ResponseEntity.ok(service.update(symbol, request));
    }

    @PatchMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> patch(
            @PathVariable String symbol, @Valid @RequestBody InstrumentPatchRequest request) {
        return ResponseEntity.ok(service.patch(symbol, request));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<InstrumentResponse> delete(@PathVariable String symbol) {
        return ResponseEntity.ok(service.delete(symbol));
    }
}
