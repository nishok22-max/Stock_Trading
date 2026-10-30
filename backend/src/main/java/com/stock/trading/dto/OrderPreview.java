package com.stock.trading.dto;

import com.stock.trading.service.BrokerageService.Charges;
import java.math.BigDecimal;
import java.time.Instant;

public record OrderPreview(PlaceOrderRequest order, BigDecimal currentPrice, BigDecimal reservationPrice,
                           Charges charges, BigDecimal cashToReserve, int sharesToReserve,
                           boolean executesImmediately, Instant quotedAt, Instant expiresAt) {}
