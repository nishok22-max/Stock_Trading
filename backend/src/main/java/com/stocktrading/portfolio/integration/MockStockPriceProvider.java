package com.stocktrading.portfolio.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Mock implementation of StockPriceProvider.
 *
 * Allows Member 4 (Portfolio & Watchlist) to run standalone and independently
 * before integrating with Member 2's live Stock and Market service.
 */
@Component
public class MockStockPriceProvider implements StockPriceProvider {

    private final Map<String, BigDecimal> marketPrices = new ConcurrentHashMap<>();

    public MockStockPriceProvider() {
        // Initialize default realistic stock prices (INR / USD)
        marketPrices.put("TCS", new BigDecimal("3920.50"));
        marketPrices.put("INFY", new BigDecimal("1845.00"));
        marketPrices.put("RELIANCE", new BigDecimal("2980.75"));
        marketPrices.put("HDFCBANK", new BigDecimal("1680.20"));
        marketPrices.put("ICICIBANK", new BigDecimal("1240.60"));
        marketPrices.put("TATAMOTORS", new BigDecimal("965.30"));
        marketPrices.put("ITC", new BigDecimal("492.10"));
        marketPrices.put("WIPRO", new BigDecimal("545.80"));
        marketPrices.put("AAPL", new BigDecimal("230.50"));
        marketPrices.put("MSFT", new BigDecimal("445.00"));
        marketPrices.put("GOOGL", new BigDecimal("178.20"));
        marketPrices.put("NVDA", new BigDecimal("135.40"));
        marketPrices.put("TSLA", new BigDecimal("250.80"));
    }

    @Override
    public BigDecimal getCurrentPrice(String stockSymbol) {
        if (stockSymbol == null || stockSymbol.isBlank()) {
            throw new IllegalArgumentException("Stock symbol cannot be null or blank");
        }
        String normalized = stockSymbol.trim().toUpperCase();
        BigDecimal price = marketPrices.get(normalized);
        if (price == null) {
            // Default fallback price if any arbitrary valid ticker is queried during testing
            return new BigDecimal("100.00");
        }
        return price;
    }

    @Override
    public boolean isValidStock(String stockSymbol) {
        if (stockSymbol == null || stockSymbol.isBlank()) {
            return false;
        }
        String normalized = stockSymbol.trim().toUpperCase();
        // A recognized stock symbol or any clean alphanumeric ticker between 1 and 10 chars
        return marketPrices.containsKey(normalized) || normalized.matches("^[A-Z0-9]{1,10}$");
    }

    @Override
    public Map<String, BigDecimal> getPrices(Iterable<String> stockSymbols) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (stockSymbols != null) {
            for (String symbol : stockSymbols) {
                if (symbol != null && !symbol.isBlank()) {
                    result.put(symbol.trim().toUpperCase(), getCurrentPrice(symbol));
                }
            }
        }
        return result;
    }

    /**
     * Helper method to set or update market price dynamically (useful for tests and simulations).
     */
    public void setPrice(String stockSymbol, BigDecimal price) {
        if (stockSymbol != null && price != null) {
            marketPrices.put(stockSymbol.trim().toUpperCase(), price);
        }
    }
}
