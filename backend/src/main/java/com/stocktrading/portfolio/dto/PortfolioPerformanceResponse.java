package com.stocktrading.portfolio.dto;

import java.math.BigDecimal;

/**
 * DTO providing comprehensive analytical performance metrics for a user's portfolio.
 */
public class PortfolioPerformanceResponse {

    private Long userId;
    private BigDecimal totalInvestedValue;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalUnrealizedProfitLoss;
    private BigDecimal totalReturnPercentage;
    private int totalHoldingsCount;
    private int profitableHoldingsCount;
    private int lossMakingHoldingsCount;
    private String bestPerformerSymbol;
    private BigDecimal bestPerformerReturnPercentage;
    private String worstPerformerSymbol;
    private BigDecimal worstPerformerReturnPercentage;

    public PortfolioPerformanceResponse() {
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

    public int getTotalHoldingsCount() {
        return totalHoldingsCount;
    }

    public void setTotalHoldingsCount(int totalHoldingsCount) {
        this.totalHoldingsCount = totalHoldingsCount;
    }

    public int getProfitableHoldingsCount() {
        return profitableHoldingsCount;
    }

    public void setProfitableHoldingsCount(int profitableHoldingsCount) {
        this.profitableHoldingsCount = profitableHoldingsCount;
    }

    public int getLossMakingHoldingsCount() {
        return lossMakingHoldingsCount;
    }

    public void setLossMakingHoldingsCount(int lossMakingHoldingsCount) {
        this.lossMakingHoldingsCount = lossMakingHoldingsCount;
    }

    public String getBestPerformerSymbol() {
        return bestPerformerSymbol;
    }

    public void setBestPerformerSymbol(String bestPerformerSymbol) {
        this.bestPerformerSymbol = bestPerformerSymbol;
    }

    public BigDecimal getBestPerformerReturnPercentage() {
        return bestPerformerReturnPercentage;
    }

    public void setBestPerformerReturnPercentage(BigDecimal bestPerformerReturnPercentage) {
        this.bestPerformerReturnPercentage = bestPerformerReturnPercentage;
    }

    public String getWorstPerformerSymbol() {
        return worstPerformerSymbol;
    }

    public void setWorstPerformerSymbol(String worstPerformerSymbol) {
        this.worstPerformerSymbol = worstPerformerSymbol;
    }

    public BigDecimal getWorstPerformerReturnPercentage() {
        return worstPerformerReturnPercentage;
    }

    public void setWorstPerformerReturnPercentage(BigDecimal worstPerformerReturnPercentage) {
        this.worstPerformerReturnPercentage = worstPerformerReturnPercentage;
    }
}
