package com.stock.trading.port;
import java.time.Instant;
import java.util.List;
public interface MarketGateway {
    StockQuote quote(String symbol);
    List<StockQuote> stocks();
    boolean isOpen(Instant now);
    Instant sessionClose(Instant now);
}
