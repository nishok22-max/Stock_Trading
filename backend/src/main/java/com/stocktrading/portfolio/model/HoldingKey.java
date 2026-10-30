package com.stocktrading.portfolio.model;

import java.util.Objects;

/**
 * Composite key representing the unique identification of a stock holding
 * for a specific user.
 *
 * Demonstrates Java 21 Record for clean, immutable composite keys
 * with automatic equals() and hashCode() implementations.
 */
public record HoldingKey(Long userId, String stockSymbol) {

    public HoldingKey {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(stockSymbol, "stockSymbol must not be null");
        if (stockSymbol.isBlank()) {
            throw new IllegalArgumentException("stockSymbol must not be blank");
        }
        // Normalize symbol to uppercase to avoid case-sensitive duplicate keys
        stockSymbol = stockSymbol.trim().toUpperCase();
    }

    /**
     * Helper factory method to create a composite key from a Holding entity.
     */
    public static HoldingKey of(Long userId, String stockSymbol) {
        return new HoldingKey(userId, stockSymbol);
    }
}
