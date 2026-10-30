package com.stock.trading.dto;

import com.stock.trading.enums.*;
import com.stock.trading.model.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(UUID id, UUID userId, String symbol, OrderSide side, OrderType type, int quantity,
                            BigDecimal limitPrice, OrderStatus status, BigDecimal reservedCash, int reservedShares,
                            BigDecimal executionPrice, BigDecimal brokerage, BigDecimal gst, UUID tradeId,
                            String reason, Instant createdAt, Instant updatedAt, Instant expiresAt) {
    public static OrderResponse from(Order o) {
        return new OrderResponse(o.id(),o.userId(),o.symbol(),o.side(),o.type(),o.quantity(),o.limitPrice(),o.status(),
                o.reservedCash(),o.reservedShares(),o.executionPrice(),o.brokerage(),o.gst(),o.tradeId(),o.reason(),o.createdAt(),o.updatedAt(),o.expiresAt());
    }
}
