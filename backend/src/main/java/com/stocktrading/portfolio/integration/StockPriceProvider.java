package com.stocktrading.portfolio.integration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Integration boundary interface to Member 2's Stock and Market module.
 *
 * Provides real-time or cached stock market prices and stock symbol validation.
 */
public interface StockPriceProvider {

    /**
     * Retrieves the current market price for a given stock symbol.
     * @param stockSymbol the ticker symbol (e.g., "TCS", "INFY")
     * @return current market price
     * @throws IllegalArgumentException if symbol is invalid or not found
     */
    BigDecimal getCurrentPrice(String stockSymbol);

    /**
     * Checks whether a stock symbol is a valid, recognized tradeable asset.
     */
    boolean isValidStock(String stockSymbol);

    /**
     * Batch fetch prices for multiple stock symbols.
     */
    Map<String, BigDecimal> getPrices(Iterable<String> stockSymbols);
}
