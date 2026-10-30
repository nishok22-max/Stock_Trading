package com.stock.trading.port;
import java.math.BigDecimal;
public record StockQuote(String symbol, String companyName, String sector, BigDecimal price,
                         BigDecimal previousClose, boolean active) {}
