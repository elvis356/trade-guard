package com.tradeguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "surveillance")
public class SurveillanceProperties {

    private Duration window = Duration.ofMinutes(5);
    private BigDecimal closingPriceDeviationPct = new BigDecimal("5");
    private long closingMinimumQuantity = 5000;
    private int closingWindowMinutes = 5;

    @PostConstruct
    void validate() {
        if (window == null || window.isZero() || window.isNegative()
                || closingPriceDeviationPct == null
                || closingPriceDeviationPct.signum() <= 0
                || closingPriceDeviationPct.compareTo(BigDecimal.valueOf(100)) > 0
                || closingMinimumQuantity <= 0
                || closingWindowMinutes <= 0 || closingWindowMinutes > 1440) {
            throw new IllegalStateException("Surveillance thresholds must be positive and valid.");
        }
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }

    public BigDecimal getClosingPriceDeviationPct() {
        return closingPriceDeviationPct;
    }

    public void setClosingPriceDeviationPct(BigDecimal closingPriceDeviationPct) {
        this.closingPriceDeviationPct = closingPriceDeviationPct;
    }

    public long getClosingMinimumQuantity() {
        return closingMinimumQuantity;
    }

    public void setClosingMinimumQuantity(long closingMinimumQuantity) {
        this.closingMinimumQuantity = closingMinimumQuantity;
    }

    public int getClosingWindowMinutes() {
        return closingWindowMinutes;
    }

    public void setClosingWindowMinutes(int closingWindowMinutes) {
        this.closingWindowMinutes = closingWindowMinutes;
    }
}
