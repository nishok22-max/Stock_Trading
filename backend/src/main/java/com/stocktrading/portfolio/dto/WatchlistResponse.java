package com.stocktrading.portfolio.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for watchlist items enriched with market price.
 */
public class WatchlistResponse {

    private Long id;
    private Long userId;
    private String stockSymbol;
    private BigDecimal currentPrice;
    private LocalDateTime addedAt;

    public WatchlistResponse() {
    }

    public WatchlistResponse(Long id, Long userId, String stockSymbol, BigDecimal currentPrice, LocalDateTime addedAt) {
        this.id = id;
        this.userId = userId;
        this.stockSymbol = stockSymbol;
        this.currentPrice = currentPrice;
        this.addedAt = addedAt;
    }

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
        this.stockSymbol = stockSymbol;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
