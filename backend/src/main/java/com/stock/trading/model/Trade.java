package com.stock.trading.model;

import com.stock.trading.enums.OrderSide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Trade(UUID id, UUID orderId, UUID userId, String symbol, OrderSide side, int quantity,
                    BigDecimal price, BigDecimal value, BigDecimal brokerage, BigDecimal gst,
                    BigDecimal totalCharges, BigDecimal netCashFlow, BigDecimal realizedPnl, Instant executedAt) {}
