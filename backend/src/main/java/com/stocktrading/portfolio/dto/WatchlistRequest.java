package com.stocktrading.portfolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a stock ticker to a user's watchlist.
 */
public class WatchlistRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotBlank(message = "stockSymbol is required")
    private String stockSymbol;

    public WatchlistRequest() {
    }

    public WatchlistRequest(Long userId, String stockSymbol) {
        this.userId = userId;
        this.stockSymbol = stockSymbol;
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
}
