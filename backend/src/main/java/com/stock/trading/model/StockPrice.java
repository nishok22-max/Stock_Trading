package com.stock.trading.model;

import java.math.BigDecimal;
import java.time.Instant;

public record StockPrice(String symbol, BigDecimal price, BigDecimal previousPrice, Instant timestamp) {}
