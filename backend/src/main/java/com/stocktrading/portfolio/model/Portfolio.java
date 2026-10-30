package com.stocktrading.portfolio.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing a user's consolidated investment portfolio snapshot.
 *
 * Contains the collection of individual stock holdings and the derived
 * overall valuation metrics (invested value, current market value,
 * unrealized profit/loss, and portfolio return percentage).
 */
public class Portfolio {

    private final Long userId;
    private final List<Holding> holdings;
    private final BigDecimal totalInvestedValue;
    private final BigDecimal totalCurrentValue;
    private final BigDecimal totalUnrealizedProfitLoss;
    private final BigDecimal totalReturnPercentage;

    public Portfolio(Long userId,
                     List<Holding> holdings,
                     BigDecimal totalInvestedValue,
                     BigDecimal totalCurrentValue,
                     BigDecimal totalUnrealizedProfitLoss,
                     BigDecimal totalReturnPercentage) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.holdings = holdings != null ? new ArrayList<>(holdings) : Collections.emptyList();
        this.totalInvestedValue = totalInvestedValue != null ? totalInvestedValue : BigDecimal.ZERO;
        this.totalCurrentValue = totalCurrentValue != null ? totalCurrentValue : BigDecimal.ZERO;
        this.totalUnrealizedProfitLoss = totalUnrealizedProfitLoss != null ? totalUnrealizedProfitLoss : BigDecimal.ZERO;
        this.totalReturnPercentage = totalReturnPercentage != null ? totalReturnPercentage : BigDecimal.ZERO;
    }

    /**
     * Factory method for creating an empty portfolio for a user who has no active holdings.
     */
    public static Portfolio empty(Long userId) {
        return new Portfolio(
                userId,
                Collections.emptyList(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }

    // Getters

    public Long getUserId() {
        return userId;
    }

    public List<Holding> getHoldings() {
        return Collections.unmodifiableList(holdings);
    }

    public BigDecimal getTotalInvestedValue() {
        return totalInvestedValue;
    }

    public BigDecimal getTotalCurrentValue() {
        return totalCurrentValue;
    }

    public BigDecimal getTotalUnrealizedProfitLoss() {
        return totalUnrealizedProfitLoss;
    }

    public BigDecimal getTotalReturnPercentage() {
        return totalReturnPercentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Portfolio portfolio)) return false;
        return Objects.equals(userId, portfolio.userId) &&
               Objects.equals(holdings, portfolio.holdings) &&
               Objects.equals(totalInvestedValue, portfolio.totalInvestedValue) &&
               Objects.equals(totalCurrentValue, portfolio.totalCurrentValue) &&
               Objects.equals(totalUnrealizedProfitLoss, portfolio.totalUnrealizedProfitLoss) &&
               Objects.equals(totalReturnPercentage, portfolio.totalReturnPercentage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, holdings, totalInvestedValue, totalCurrentValue,
                totalUnrealizedProfitLoss, totalReturnPercentage);
    }

    @Override
    public String toString() {
        return "Portfolio{" +
                "userId=" + userId +
                ", holdingsCount=" + holdings.size() +
                ", totalInvestedValue=" + totalInvestedValue +
                ", totalCurrentValue=" + totalCurrentValue +
                ", totalUnrealizedProfitLoss=" + totalUnrealizedProfitLoss +
                ", totalReturnPercentage=" + totalReturnPercentage + "%" +
                '}';
    }
}
