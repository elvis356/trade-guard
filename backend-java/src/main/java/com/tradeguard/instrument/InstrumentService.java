package com.tradeguard.instrument;

import com.tradeguard.common.DuplicateResourceException;
import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.common.ResourceNotFoundException;
import com.tradeguard.instrument.dto.InstrumentPatchRequest;
import com.tradeguard.instrument.dto.InstrumentRequest;
import com.tradeguard.instrument.dto.InstrumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class InstrumentService {

    private final InstrumentRepository repository;

    public InstrumentService(InstrumentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InstrumentResponse create(InstrumentRequest request) {
        if (repository.existsBySymbol(request.symbol())) {
            throw new DuplicateResourceException("商品代碼已存在: " + request.symbol());
        }
        BigDecimal referencePrice = request.referencePrice() == null
                ? new BigDecimal("100.00") : request.referencePrice();
        Instrument instrument = new Instrument(request.symbol(), request.name(), request.active(),
                referencePrice);
        return InstrumentResponse.from(repository.saveAndFlush(instrument));
    }

    @Transactional(readOnly = true)
    public Page<InstrumentResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(InstrumentResponse::from);
    }

    @Transactional(readOnly = true)
    public InstrumentResponse findBySymbol(String symbol) {
        return InstrumentResponse.from(getBySymbol(symbol));
    }

    @Transactional
    public InstrumentResponse update(String symbol, InstrumentRequest request) {
        Instrument existing = getBySymbol(symbol);
        if (!existing.getSymbol().equals(request.symbol())
                && repository.existsBySymbol(request.symbol())) {
            throw new DuplicateResourceException("商品代碼已存在: " + request.symbol());
        }
        existing.setSymbol(request.symbol());
        existing.setName(request.name());
        existing.setActive(request.active());
        existing.setReferencePrice(request.referencePrice() == null
                ? new BigDecimal("100.00") : request.referencePrice());
        return InstrumentResponse.from(repository.saveAndFlush(existing));
    }

    @Transactional
    public InstrumentResponse patch(String symbol, InstrumentPatchRequest request) {
        if (request.name() == null && request.active() == null) {
            throw new InvalidRequestException("PATCH 至少需要提供一個要修改的欄位。");
        }
        Instrument existing = getBySymbol(symbol);
        if (request.name() != null) {
            existing.setName(request.name());
        }
        if (request.active() != null) {
            existing.setActive(request.active());
        }
        if (request.referencePrice() != null) {
            existing.setReferencePrice(request.referencePrice());
        }
        return InstrumentResponse.from(repository.saveAndFlush(existing));
    }

    @Transactional
    public InstrumentResponse delete(String symbol) {
        Instrument existing = getBySymbol(symbol);
        InstrumentResponse deleted = InstrumentResponse.from(existing);
        repository.delete(existing);
        return deleted;
    }

    private Instrument getBySymbol(String symbol) {
        return repository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("找不到商品代碼: " + symbol));
    }
}
