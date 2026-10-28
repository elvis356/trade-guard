package com.tradeguard.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum OrderSide {
    BUY("B"),
    SELL("S");

    private final String code;

    OrderSide(String code) {
        this.code = code;
    }

    @JsonValue
    public String code() {
        return code;
    }

    @JsonCreator
    public static OrderSide from(String value) {
        return switch (value.toUpperCase(java.util.Locale.ROOT)) {
            case "B", "BUY" -> BUY;
            case "S", "SELL" -> SELL;
            default -> throw new IllegalArgumentException("side must be B or S");
        };
    }
}
