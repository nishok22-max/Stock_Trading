package com.stocktrading.portfolio.dto;

import java.math.BigDecimal;

/**
 * DTO representing an individual holding enriched with real-time market data,
 * valuation, unrealized P&L, and portfolio weight allocation.
 */
public class HoldingResponse {

    private String stockSymbol;
    private Integer quantity;
    private BigDecimal averagePurchasePrice;
    private BigDecimal currentMarketPrice;
    private BigDecimal investedValue;
    private BigDecimal currentValue;
    private BigDecimal unrealizedProfitLoss;
    private BigDecimal returnPercentage;
    private BigDecimal allocationPercentage;

    public HoldingResponse() {
    }

    public HoldingResponse(String stockSymbol,
                           Integer quantity,
                           BigDecimal averagePurchasePrice,
                           BigDecimal currentMarketPrice,
                           BigDecimal investedValue,
                           BigDecimal currentValue,
                           BigDecimal unrealizedProfitLoss,
                           BigDecimal returnPercentage,
                           BigDecimal allocationPercentage) {
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.currentMarketPrice = currentMarketPrice;
        this.investedValue = investedValue;
        this.currentValue = currentValue;
        this.unrealizedProfitLoss = unrealizedProfitLoss;
        this.returnPercentage = returnPercentage;
        this.allocationPercentage = allocationPercentage;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public void setStockSymbol(String stockSymbol) {
        this.stockSymbol = stockSymbol;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAveragePurchasePrice() {
        return averagePurchasePrice;
    }

    public void setAveragePurchasePrice(BigDecimal averagePurchasePrice) {
        this.averagePurchasePrice = averagePurchasePrice;
    }

    public BigDecimal getCurrentMarketPrice() {
        return currentMarketPrice;
    }

    public void setCurrentMarketPrice(BigDecimal currentMarketPrice) {
        this.currentMarketPrice = currentMarketPrice;
    }

    public BigDecimal getInvestedValue() {
        return investedValue;
    }

    public void setInvestedValue(BigDecimal investedValue) {
        this.investedValue = investedValue;
    }

    public BigDecimal getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(BigDecimal currentValue) {
        this.currentValue = currentValue;
    }

    public BigDecimal getUnrealizedProfitLoss() {
        return unrealizedProfitLoss;
    }

    public void setUnrealizedProfitLoss(BigDecimal unrealizedProfitLoss) {
        this.unrealizedProfitLoss = unrealizedProfitLoss;
    }

    public BigDecimal getReturnPercentage() {
        return returnPercentage;
    }

    public void setReturnPercentage(BigDecimal returnPercentage) {
        this.returnPercentage = returnPercentage;
    }

    public BigDecimal getAllocationPercentage() {
        return allocationPercentage;
    }

    public void setAllocationPercentage(BigDecimal allocationPercentage) {
        this.allocationPercentage = allocationPercentage;
    }
}
