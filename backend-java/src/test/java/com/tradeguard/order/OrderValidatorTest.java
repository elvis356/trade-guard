package com.tradeguard.order;

import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.config.MarketProperties;
import com.tradeguard.instrument.Instrument;
import com.tradeguard.order.dto.CreateOrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderValidatorTest {

    private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");

    @Test
    void acceptsTickValuesAtEachPriceBandAndRejectsOffTickPrices() {
        MarketProperties properties = new MarketProperties();
        properties.setEnforceTradingHours(false);
        properties.setPriceLimitPct(new BigDecimal("100"));
        OrderValidator validator = new OrderValidator(properties, Clock.systemUTC());
        Instrument instrument = new Instrument("TEST01", "測試商品", true,
                new BigDecimal("10000"));

        for (String validPrice : new String[] {
                "9.99", "10.00", "10.05", "50.00", "50.10",
                "100.00", "100.50", "500.00", "501.00", "1000.00", "1005.00"
        }) {
            assertDoesNotThrow(() -> validator.validate(request(validPrice, 1000), instrument),
                    validPrice);
        }

        for (String invalidPrice : new String[] {
                "10.01", "50.05", "100.10", "500.50", "1001.00"
        }) {
            assertThrows(InvalidRequestException.class,
                    () -> validator.validate(request(invalidPrice, 1000), instrument),
                    invalidPrice);
        }
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(request("10.00", 500), instrument));
    }

    @Test
    void tradingHoursAreWeekdaysFromOpenInclusiveToCloseExclusiveInTaipeiTime() {
        MarketProperties properties = new MarketProperties();
        Instrument instrument = new Instrument("TEST01", "測試商品", true,
                new BigDecimal("100"));

        assertValidAt(properties, instrument, "2026-10-28T09:00:00+08:00");
        assertValidAt(properties, instrument, "2026-10-28T13:29:59+08:00");
        assertInvalidAt(properties, instrument, "2026-10-28T08:59:59+08:00");
        assertInvalidAt(properties, instrument, "2026-10-28T13:30:00+08:00");
        assertInvalidAt(properties, instrument, "2026-10-31T10:00:00+08:00");
    }

    private void assertValidAt(MarketProperties properties, Instrument instrument, String time) {
        OrderValidator validator = validatorAt(properties, time);
        assertDoesNotThrow(() -> validator.validate(request("100.00", 1000), instrument));
    }

    private void assertInvalidAt(MarketProperties properties, Instrument instrument, String time) {
        OrderValidator validator = validatorAt(properties, time);
        assertThrows(InvalidRequestException.class,
                () -> validator.validate(request("100.00", 1000), instrument));
    }

    private OrderValidator validatorAt(MarketProperties properties, String localTime) {
        Instant instant = java.time.OffsetDateTime.parse(localTime).toInstant();
        return new OrderValidator(properties, Clock.fixed(instant, TAIPEI));
    }

    private CreateOrderRequest request(String price, long quantity) {
        return new CreateOrderRequest(UUID.randomUUID(), "TEST01", OrderSide.BUY,
                new BigDecimal(price), quantity);
    }
}
