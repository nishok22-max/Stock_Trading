package com.stock.trading.model;

import com.stock.trading.enums.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable intent. Terminal orders cannot be changed or executed again. */
public record Order(UUID id, UUID userId, String symbol, OrderSide side, OrderType type,
                    int quantity, BigDecimal limitPrice, OrderStatus status,
                    BigDecimal reservedCash, int reservedShares, BigDecimal executionPrice,
                    BigDecimal brokerage, BigDecimal gst, UUID tradeId, String reason,
                    Instant createdAt, Instant updatedAt, Instant expiresAt) {
    public static Order pending(UUID userId, String symbol, OrderSide side, OrderType type,
                                int quantity, BigDecimal limitPrice, Instant now, Instant expiresAt) {
        return new Order(UUID.randomUUID(), userId, symbol, side, type, quantity, limitPrice,
                OrderStatus.PENDING, Money.ZERO, 0, null, Money.ZERO, Money.ZERO, null, null, now, now, expiresAt);
    }
    public boolean canExecute(BigDecimal price) {
        return status == OrderStatus.PENDING && ExecutionRule.forType(type).matches(side,price,limitPrice);
    }
    public Order reserved(BigDecimal cash, int shares) {
        requirePending();
        return new Order(id,userId,symbol,side,type,quantity,limitPrice,status,cash,shares,
                executionPrice,brokerage,gst,tradeId,reason,createdAt,updatedAt,expiresAt);
    }
    public Order executed(Trade trade) {
        requirePending();
        return new Order(id,userId,symbol,side,type,quantity,limitPrice,OrderStatus.EXECUTED,
                Money.ZERO,0,trade.price(),trade.brokerage(),trade.gst(),trade.id(),null,createdAt,trade.executedAt(),expiresAt);
    }
    public Order closed(OrderStatus target, String reason, Instant now) {
        requirePending();
        if (target != OrderStatus.CANCELLED && target != OrderStatus.REJECTED) throw new IllegalArgumentException("Invalid terminal status");
        return new Order(id,userId,symbol,side,type,quantity,limitPrice,target,Money.ZERO,0,null,
                Money.ZERO,Money.ZERO,null,reason,createdAt,now,expiresAt);
    }
    private void requirePending() {
        if (status != OrderStatus.PENDING) throw new IllegalStateException("Order is already terminal");
    }
}
