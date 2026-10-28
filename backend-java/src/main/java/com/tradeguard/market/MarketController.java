package com.tradeguard.market;

import com.tradeguard.instrument.InstrumentService;
import com.tradeguard.instrument.dto.InstrumentResponse;
import com.tradeguard.order.OrderService;
import com.tradeguard.order.dto.TradeResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final InstrumentService instrumentService;
    private final OrderService orderService;
    private final MarketQueryService marketQueryService;

    public MarketController(InstrumentService instrumentService, OrderService orderService,
                            MarketQueryService marketQueryService) {
        this.instrumentService = instrumentService;
        this.orderService = orderService;
        this.marketQueryService = marketQueryService;
    }

    @GetMapping("/{symbol}/info")
    public ResponseEntity<InstrumentResponse> getInstrumentInfo(@PathVariable String symbol) {
        return ResponseEntity.ok(instrumentService.findBySymbol(symbol));
    }

    @GetMapping("/quotes")
    public ResponseEntity<java.util.List<MarketQuote>> getQuotes() {
        return ResponseEntity.ok(marketQueryService.getQuotes());
    }

    @GetMapping("/{symbol}/book")
    public ResponseEntity<BookSnapshot> getOrderBook(@PathVariable String symbol) {
        return ResponseEntity.ok(marketQueryService.getOrderBook(symbol));
    }

    @GetMapping("/{symbol}/trades")
    public ResponseEntity<TradeResponse.Page> getRecentTrades(
            @PathVariable String symbol,
            @PageableDefault(size = 20,
                    sort = "id",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(TradeResponse.Page.from(
                orderService.findTrades(symbol, pageable)));
    }
}
