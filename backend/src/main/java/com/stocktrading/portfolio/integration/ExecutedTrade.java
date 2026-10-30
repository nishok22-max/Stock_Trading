package com.stocktrading.portfolio.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO received across the integration boundary from Member 5 (Trades Module).
 *
 * Encapsulates the details of a successfully executed trade.
 * Only successfully executed trades should update holdings.
 */
public class ExecutedTrade {

    private Long tradeId;
    private Long userId;
    private String stockSymbol;
    private TradeSide side;
    private Integer quantity;
    private BigDecimal executionPrice;
    private LocalDateTime executedAt;

    public ExecutedTrade() {
    }

    public ExecutedTrade(Long tradeId,
                         Long userId,
                         String stockSymbol,
                         TradeSide side,
                         Integer quantity,
                         BigDecimal executionPrice,
                         LocalDateTime executedAt) {
        this.tradeId = tradeId;
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol must not be null").trim().toUpperCase();
        this.side = Objects.requireNonNull(side, "trade side must not be null");
        this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
        this.executionPrice = Objects.requireNonNull(executionPrice, "executionPrice must not be null");
        this.executedAt = executedAt != null ? executedAt : LocalDateTime.now();
    }

    public Long getTradeId() {
        return tradeId;
    }

    public void setTradeId(Long tradeId) {
        this.tradeId = tradeId;
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

    public TradeSide getSide() {
        return side;
    }

    public void setSide(TradeSide side) {
        this.side = side;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getExecutionPrice() {
        return executionPrice;
    }

    public void setExecutionPrice(BigDecimal executionPrice) {
        this.executionPrice = executionPrice;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    @Override
    public String toString() {
        return "ExecutedTrade{" +
                "tradeId=" + tradeId +
                ", userId=" + userId +
                ", stockSymbol='" + stockSymbol + '\'' +
                ", side=" + side +
                ", quantity=" + quantity +
                ", executionPrice=" + executionPrice +
                ", executedAt=" + executedAt +
                '}';
    }
}
