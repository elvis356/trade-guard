package com.tradeguard.surveillance;

import com.tradeguard.order.Trade;
import com.tradeguard.order.TradeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
public class TradeWindowStore {

    private static final Logger log = LoggerFactory.getLogger(TradeWindowStore.class);
    private static final String PREFIX = "tradeguard:surveillance:window:";

    private final StringRedisTemplate redis;
    private final TradeRepository trades;

    public TradeWindowStore(StringRedisTemplate redis, TradeRepository trades) {
        this.redis = redis;
        this.trades = trades;
    }

    public List<Trade> recordAndRead(String symbol, Trade trade, Duration window) {
        Instant cutoff = trade.getCreatedAt().minus(window);
        String key = PREFIX + symbol;
        try {
            var sortedSet = redis.opsForZSet();
            sortedSet.removeRangeByScore(key, Double.NEGATIVE_INFINITY,
                    cutoff.toEpochMilli());
            sortedSet.add(key, trade.getId().toString(), trade.getCreatedAt().toEpochMilli());
            redis.expire(key, window.plus(Duration.ofMinutes(1)));
            Set<String> ids = sortedSet.rangeByScore(key, cutoff.toEpochMilli(),
                    Double.POSITIVE_INFINITY);
            if (ids == null || ids.isEmpty()) {
                return List.of(trade);
            }
            return trades.findAllById(ids.stream().map(Long::valueOf).toList());
        } catch (DataAccessException | NumberFormatException ex) {
            log.warn("Redis surveillance window unavailable for {}; scanning persisted trades",
                    symbol, ex);
            return trades.findAllByOrderByIdAsc().stream()
                    .filter(item -> item.getSymbol().equals(symbol)
                            && !item.getCreatedAt().isBefore(cutoff))
                    .toList();
        }
    }
}
