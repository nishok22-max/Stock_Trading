package com.stock.trading.dashboard.dto;

import com.stock.trading.dto.OrderResponse;
import com.stock.trading.enums.OrderSide;
import com.stock.trading.enums.OrderStatus;
import com.stock.trading.enums.OrderType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Details of a recent order for dashboard display")
public record RecentOrderResponse(
    @Schema(description = "Unique order identifier")
    UUID orderId,

    @Schema(description = "Stock symbol (e.g. TCS, INFY)")
    String stockSymbol,

    @Schema(description = "Order side: BUY or SELL")
    OrderSide side,

    @Schema(description = "Order type: MARKET or LIMIT")
    OrderType orderType,

    @Schema(description = "Quantity of shares ordered")
    int quantity,

    @Schema(description = "Limit price if limit order")
    BigDecimal limitPrice,

    @Schema(description = "Execution price if executed")
    BigDecimal executionPrice,

    @Schema(description = "Order status: PENDING, EXECUTED, CANCELLED, or REJECTED")
    OrderStatus status,

    @Schema(description = "Order creation timestamp")
    Instant createdAt,

    @Schema(description = "Order last updated timestamp")
    Instant updatedAt
) {
    public static RecentOrderResponse from(OrderResponse o) {
        return new RecentOrderResponse(
            o.id(),
            o.symbol(),
            o.side(),
            o.type(),
            o.quantity(),
            o.limitPrice(),
            o.executionPrice(),
            o.status(),
            o.createdAt(),
            o.updatedAt()
        );
    }
}

