package com.stock.trading.port;
import java.math.BigDecimal;
import java.util.UUID;
public record HoldingSnapshot(UUID userId, String symbol, int quantity, int reservedQuantity, BigDecimal averageBuyPrice) {
    public int availableQuantity() { return quantity - reservedQuantity; }
}
