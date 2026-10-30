package com.stock.trading.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Aggregated dashboard summary for a trader")
public record DashboardResponse(
    @Schema(description = "User unique identifier")
    UUID userId,

    @Schema(description = "Full name of the account holder")
    String name,

    @Schema(description = "Whether the user's KYC verification is approved")
    boolean kycApproved,

    @Schema(description = "Whether trading is restricted or blocked on this account")
    boolean blocked,

    @Schema(description = "Available cash balance in INR ready for new orders")
    BigDecimal availableBalance,

    @Schema(description = "Cash balance in INR currently blocked/reserved for pending orders")
    BigDecimal blockedBalance,

    @Schema(description = "Total amount invested across all current stock holdings in INR")
    BigDecimal totalInvestedValue,

    @Schema(description = "Current market value of all held stocks in INR")
    BigDecimal currentPortfolioValue,

    @Schema(description = "Total unrealized profit or loss in INR")
    BigDecimal totalProfitLoss,

    @Schema(description = "Total unrealized profit or loss as a percentage of invested amount")
    BigDecimal totalProfitLossPercentage,

    @Schema(description = "Whether the market is currently open for active trading")
    boolean marketOpen,

    @Schema(description = "Total number of stock holdings currently in portfolio")
    int totalHoldings,

    @Schema(description = "Total number of orders placed by this user")
    int totalOrders,

    @Schema(description = "Number of orders currently in PENDING state")
    int pendingOrders,

    @Schema(description = "List of up to 10 most recent orders placed by the user")
    List<RecentOrderResponse> recentOrders,

    @Schema(description = "Timestamp when this dashboard snapshot was generated")
    Instant lastUpdated
) {}

