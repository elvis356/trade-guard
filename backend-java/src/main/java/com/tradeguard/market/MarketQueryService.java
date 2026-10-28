package com.tradeguard.market;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradeguard.common.ResourceNotFoundException;
import com.tradeguard.instrument.Instrument;
import com.tradeguard.instrument.InstrumentRepository;
import com.tradeguard.order.OrderStatus;
import com.tradeguard.order.Trade;
import com.tradeguard.order.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MarketQueryService {

    private static final Logger log = LoggerFactory.getLogger(MarketQueryService.class);
    private static final String QUOTE_PREFIX = "tradeguard:quote:";
    private static final String BOOK_PREFIX = "tradeguard:book:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(15);
    private static final int BOOK_DEPTH = 10;

    private final InstrumentRepository instruments;
    private final TradeRepository trades;
    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public MarketQueryService(InstrumentRepository instruments, TradeRepository trades,
                              JdbcTemplate jdbc, StringRedisTemplate redis,
                              ObjectMapper objectMapper) {
        this.instruments = instruments;
        this.trades = trades;
        this.jdbc = jdbc;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public List<MarketQuote> getQuotes() {
        List<MarketQuote> quotes = instruments.findByActiveTrueOrderBySymbolAsc().stream()
                .map(instrument -> getQuote(instrument))
                .toList();
        return quotes;
    }

    public BookSnapshot getOrderBook(String symbol) {
        Instrument instrument = instruments.findBySymbol(symbol)
                .filter(Instrument::isActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "找不到啟用中的商品代碼: " + symbol));
        return cached(BOOK_PREFIX + symbol, BookSnapshot.class,
                () -> loadBook(instrument.getSymbol()));
    }

    private MarketQuote getQuote(Instrument instrument) {
        return cached(QUOTE_PREFIX + instrument.getSymbol(), MarketQuote.class, () -> {
            Optional<Trade> latest = trades.findFirstBySymbolOrderByIdDesc(
                    instrument.getSymbol());
            return latest.map(trade -> new MarketQuote(instrument.getSymbol(),
                            instrument.getName(), instrument.getReferencePrice(),
                            trade.getPrice(), trade.getQuantity(), trade.getCreatedAt()))
                    .orElseGet(() -> new MarketQuote(instrument.getSymbol(),
                            instrument.getName(), instrument.getReferencePrice(),
                            null, 0, null));
        });
    }

    private BookSnapshot loadBook(String symbol) {
        List<BookLevel> bids = new ArrayList<>();
        List<BookLevel> asks = new ArrayList<>();
        jdbc.query("""
                SELECT side, limit_price, SUM(remaining_quantity) AS total_quantity
                FROM orders
                WHERE symbol = ? AND status IN (?, ?) AND remaining_quantity > 0
                GROUP BY side, limit_price
                """, (org.springframework.jdbc.core.RowCallbackHandler)
                result -> addLevel(result, bids, asks), symbol,
                OrderStatus.NEW.name(), OrderStatus.PARTIALLY_FILLED.name());
        bids.sort(Comparator.comparing(BookLevel::price).reversed());
        asks.sort(Comparator.comparing(BookLevel::price));
        return new BookSnapshot(symbol, List.copyOf(bids.stream().limit(BOOK_DEPTH).toList()),
                List.copyOf(asks.stream().limit(BOOK_DEPTH).toList()), Instant.now());
    }

    private void addLevel(ResultSet row, List<BookLevel> bids, List<BookLevel> asks)
            throws SQLException {
        BookLevel level = new BookLevel(row.getBigDecimal("limit_price"),
                row.getLong("total_quantity"));
        ("BUY".equals(row.getString("side")) ? bids : asks).add(level);
    }

    private <T> T cached(String key, Class<T> type,
                         java.util.function.Supplier<T> databaseLoader) {
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, type);
                } catch (JsonProcessingException ex) {
                    log.warn("Ignoring malformed market cache entry {}", key, ex);
                    redis.delete(key);
                }
            }
        } catch (DataAccessException ex) {
            log.warn("Market Redis cache unavailable; loading {} from PostgreSQL", key, ex);
        }

        T value = databaseLoader.get();
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (DataAccessException | JsonProcessingException ex) {
            log.warn("Could not refresh market cache {}", key, ex);
        }
        return value;
    }
}
