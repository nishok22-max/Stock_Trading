package com.stock.trading.port;
import java.math.BigDecimal;
import java.util.UUID;
public record AccountSnapshot(UUID userId, String name, boolean kycApproved, boolean blocked,
                              BigDecimal availableBalance, BigDecimal blockedBalance) {}
