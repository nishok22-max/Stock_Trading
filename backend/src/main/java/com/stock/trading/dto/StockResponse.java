package com.stock.trading.dto;

import com.stock.trading.port.StockQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;

public record StockResponse(
        String symbol,
        String companyName,
        String sector,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal priceChange,
        BigDecimal percentageChange,
        String status) {
    public static StockResponse from(StockQuote quote) {
        var change = quote.price().subtract(quote.previousClose()).setScale(2, RoundingMode.HALF_UP);
        var percentage = change.multiply(new BigDecimal("100"))
                .divide(quote.previousClose(), 2, RoundingMode.HALF_UP);
        return new StockResponse(quote.symbol(), quote.companyName(), quote.sector(), quote.price(),
                quote.previousClose(), change, percentage, quote.active() ? "ACTIVE" : "SUSPENDED");
    }
}
