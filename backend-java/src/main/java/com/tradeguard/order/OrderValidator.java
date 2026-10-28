package com.tradeguard.order;

import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.config.MarketProperties;
import com.tradeguard.instrument.Instrument;
import com.tradeguard.order.dto.CreateOrderRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

@Component
public class OrderValidator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final List<TickBand> TICK_BANDS = List.of(
            new TickBand(BigDecimal.ZERO, new BigDecimal("10"), new BigDecimal("0.01")),
            new TickBand(new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("0.05")),
            new TickBand(new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("0.1")),
            new TickBand(new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("0.5")),
            new TickBand(new BigDecimal("500"), new BigDecimal("1000"), BigDecimal.ONE),
            new TickBand(new BigDecimal("1000"), null, new BigDecimal("5"))
    );

    private final MarketProperties properties;
    private final Clock clock;

    public OrderValidator(MarketProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void validate(CreateOrderRequest request, Instrument instrument) {
        if (request.qty() % properties.getLotSize() != 0) {
            throw new InvalidRequestException(
                    "委託數量必須為交易單位 " + properties.getLotSize() + " 的倍數。");
        }
        validateTick(request.price());
        validatePriceLimit(request.price(), instrument.getReferencePrice());
        validateTradingHours();
    }

    private void validateTick(BigDecimal price) {
        TickBand band = TICK_BANDS.stream()
                .filter(candidate -> price.compareTo(candidate.minimum()) >= 0
                        && (candidate.maximum() == null
                        || price.compareTo(candidate.maximum()) < 0))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("委託價格未落在有效升降單位區間。"));
        if (price.remainder(band.tick()).signum() != 0) {
            throw new InvalidRequestException(
                    "委託價格不符合升降單位，該價格區間的升降單位為 " + band.tick() + "。");
        }
    }

    private void validatePriceLimit(BigDecimal price, BigDecimal referencePrice) {
        BigDecimal ratio = properties.getPriceLimitPct().divide(HUNDRED);
        BigDecimal lowerLimit = referencePrice.multiply(BigDecimal.ONE.subtract(ratio));
        BigDecimal upperLimit = referencePrice.multiply(BigDecimal.ONE.add(ratio));
        if (price.compareTo(lowerLimit) < 0 || price.compareTo(upperLimit) > 0) {
            throw new InvalidRequestException("委託價格超出參考收盤價 "
                    + properties.getPriceLimitPct().stripTrailingZeros().toPlainString()
                    + "% 的漲跌幅範圍。");
        }
    }

    private void validateTradingHours() {
        if (!properties.isEnforceTradingHours()) {
            return;
        }
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(properties.getZone());
        LocalTime time = now.toLocalTime();
        if (now.getDayOfWeek().getValue() > 5
                || time.isBefore(properties.getOpen())
                || !time.isBefore(properties.getClose())) {
            throw new InvalidRequestException("目前非交易時段（週一至週五 "
                    + properties.getOpen() + "–" + properties.getClose() + "）。");
        }
    }

    private record TickBand(BigDecimal minimum, BigDecimal maximum, BigDecimal tick) {
    }
}
