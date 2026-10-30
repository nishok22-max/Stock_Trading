package com.stocktrading.portfolio.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO representing a user's entire consolidated portfolio response.
 */
public class PortfolioResponse {

    private Long userId;
    private BigDecimal totalInvestedValue;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalUnrealizedProfitLoss;
    private BigDecimal totalReturnPercentage;
    private Integer totalHoldingsCount;
    private List<HoldingResponse> holdings;

    public PortfolioResponse() {
    }

    public PortfolioResponse(Long userId,
                             BigDecimal totalInvestedValue,
                             BigDecimal totalCurrentValue,
                             BigDecimal totalUnrealizedProfitLoss,
                             BigDecimal totalReturnPercentage,
                             Integer totalHoldingsCount,
                             List<HoldingResponse> holdings) {
        this.userId = userId;
        this.totalInvestedValue = totalInvestedValue;
        this.totalCurrentValue = totalCurrentValue;
        this.totalUnrealizedProfitLoss = totalUnrealizedProfitLoss;
        this.totalReturnPercentage = totalReturnPercentage;
        this.totalHoldingsCount = totalHoldingsCount;
        this.holdings = holdings;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalInvestedValue() {
        return totalInvestedValue;
    }

    public void setTotalInvestedValue(BigDecimal totalInvestedValue) {
        this.totalInvestedValue = totalInvestedValue;
    }

    public BigDecimal getTotalCurrentValue() {
        return totalCurrentValue;
    }

    public void setTotalCurrentValue(BigDecimal totalCurrentValue) {
        this.totalCurrentValue = totalCurrentValue;
    }

    public BigDecimal getTotalUnrealizedProfitLoss() {
        return totalUnrealizedProfitLoss;
    }

    public void setTotalUnrealizedProfitLoss(BigDecimal totalUnrealizedProfitLoss) {
        this.totalUnrealizedProfitLoss = totalUnrealizedProfitLoss;
    }

    public BigDecimal getTotalReturnPercentage() {
        return totalReturnPercentage;
    }

    public void setTotalReturnPercentage(BigDecimal totalReturnPercentage) {
        this.totalReturnPercentage = totalReturnPercentage;
    }

    public Integer getTotalHoldingsCount() {
        return totalHoldingsCount;
    }

    public void setTotalHoldingsCount(Integer totalHoldingsCount) {
        this.totalHoldingsCount = totalHoldingsCount;
    }

    public List<HoldingResponse> getHoldings() {
        return holdings;
    }

    public void setHoldings(List<HoldingResponse> holdings) {
        this.holdings = holdings;
    }
}
