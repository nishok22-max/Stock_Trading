package com.stocktrading.portfolio.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an individual stock added to a user's watchlist.
 *
 * Enforces business uniqueness per (userId, stockSymbol).
 */
public class WatchlistItem {

    private Long id;
    private Long userId;
    private String stockSymbol;
    private LocalDateTime addedAt;

    public WatchlistItem() {
    }

    public WatchlistItem(Long id, Long userId, String stockSymbol) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol must not be null").trim().toUpperCase();
        this.addedAt = LocalDateTime.now();
    }

    public WatchlistItem(Long id, Long userId, String stockSymbol, LocalDateTime addedAt) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol must not be null").trim().toUpperCase();
        this.addedAt = addedAt != null ? addedAt : LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WatchlistItem that)) return false;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(stockSymbol, that.stockSymbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, stockSymbol);
    }

    @Override
    public String toString() {
        return "WatchlistItem{" +
                "id=" + id +
                ", userId=" + userId +
                ", stockSymbol='" + stockSymbol + '\'' +
                ", addedAt=" + addedAt +
                '}';
    }
}
