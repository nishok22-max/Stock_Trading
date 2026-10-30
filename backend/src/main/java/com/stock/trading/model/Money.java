package com.stock.trading.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal ZERO = new BigDecimal("0.00");
    private Money() {}
    public static BigDecimal amount(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    public static BigDecimal value(BigDecimal price, int quantity) {
        return amount(price.multiply(BigDecimal.valueOf(quantity)));
    }
}
