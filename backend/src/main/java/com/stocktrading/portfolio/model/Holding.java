package com.stocktrading.portfolio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an individual user's consolidated holding of a specific stock.
 *
 * Encapsulates holding quantity, weighted average purchase price,
 * and lifecycle timestamps.
 */
public class Holding {

    private Long userId;
    private String stockSymbol;
    private Integer quantity;
    private BigDecimal averagePurchasePrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Holding() {
    }

    public Holding(Long userId, String stockSymbol, Integer quantity, BigDecimal averagePurchasePrice) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol must not be null").trim().toUpperCase();
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.averagePurchasePrice = Objects.requireNonNull(averagePurchasePrice, "averagePurchasePrice must not be null");
        
        if (quantity < 0) {
            throw new IllegalArgumentException("Holding quantity cannot be negative");
        }
        if (averagePurchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Average purchase price cannot be negative");
        }

        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public HoldingKey getKey() {
        return new HoldingKey(this.userId, this.stockSymbol);
    }

    // Getters and Setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol != null ? stockSymbol.trim().toUpperCase() : null;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Holding quantity cannot be negative");
        }
        this.quantity = quantity;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public void setAveragePurchasePrice(BigDecimal averagePurchasePrice) {
        if (averagePurchasePrice != null && averagePurchasePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Average purchase price cannot be negative");
        }
        this.averagePurchasePrice = averagePurchasePrice;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Holding holding)) return false;
        return Objects.equals(userId, holding.userId) &&
               Objects.equals(stockSymbol, holding.stockSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, stockSymbol);
    }

    @Override
    public String toString() {
        return "Holding{" +
                "userId=" + userId +
                ", stockSymbol='" + stockSymbol + '\'' +
                ", quantity=" + quantity +
                ", averagePurchasePrice=" + averagePurchasePrice +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
