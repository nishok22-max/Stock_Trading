package com.stock.trading.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Member 1 contract. Every call must join the engine's unit of work; no independent commits. */
public interface AccountGateway {
    AccountSnapshot account(UUID userId);
    HoldingSnapshot holding(UUID userId, String symbol);
    List<HoldingSnapshot> holdings(UUID userId);
    void reserveCash(UUID userId, BigDecimal amount);
    void releaseCash(UUID userId, BigDecimal amount);
    void reserveShares(UUID userId, String symbol, int quantity);
    void releaseShares(UUID userId, String symbol, int quantity);
    void settleBuy(UUID userId, String symbol, int quantity, BigDecimal price, BigDecimal reservation, BigDecimal charges);
    BigDecimal settleSell(UUID userId, String symbol, int quantity, BigDecimal price, BigDecimal charges);
}
