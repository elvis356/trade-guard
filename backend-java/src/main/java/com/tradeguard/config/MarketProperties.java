package com.tradeguard.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "market")
public class MarketProperties {

    private BigDecimal priceLimitPct = new BigDecimal("10");
    private long lotSize = 1000;
    private boolean enforceTradingHours = true;
    private ZoneId zone = ZoneId.of("Asia/Taipei");
    private LocalTime open = LocalTime.of(9, 0);
    private LocalTime close = LocalTime.of(13, 30);

    @PostConstruct
    void validate() {
        if (priceLimitPct == null || priceLimitPct.signum() <= 0
                || priceLimitPct.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalStateException("Market price limit must be greater than 0 and <= 100.");
        }
        if (lotSize < 1) {
            throw new IllegalStateException("Market lot size must be positive.");
        }
        if (zone == null || open == null || close == null || !open.isBefore(close)) {
            throw new IllegalStateException("Market zone and trading hours must be valid.");
        }
    }

    public BigDecimal getPriceLimitPct() {
        return priceLimitPct;
    }

    public void setPriceLimitPct(BigDecimal priceLimitPct) {
        this.priceLimitPct = priceLimitPct;
    }

    public long getLotSize() {
        return lotSize;
    }

    public void setLotSize(long lotSize) {
        this.lotSize = lotSize;
    }

    public boolean isEnforceTradingHours() {
        return enforceTradingHours;
    }

    public void setEnforceTradingHours(boolean enforceTradingHours) {
        this.enforceTradingHours = enforceTradingHours;
    }

    public ZoneId getZone() {
        return zone;
    }

    public void setZone(ZoneId zone) {
        this.zone = zone;
    }

    public LocalTime getOpen() {
        return open;
    }

    public void setOpen(LocalTime open) {
        this.open = open;
    }

    public LocalTime getClose() {
        return close;
    }

    public void setClose(LocalTime close) {
        this.close = close;
    }
}
