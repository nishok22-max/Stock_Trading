package com.stock.trading.dashboard.controller;

import com.stock.trading.dashboard.dto.DashboardResponse;
import com.stock.trading.dashboard.dto.MarketSummaryResponse;
import com.stock.trading.dashboard.dto.RecentOrderResponse;
import com.stock.trading.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard metrics, portfolio aggregation, and market summary APIs")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{userId}")
    @Operation(
        summary = "Get user dashboard summary",
        description = "Aggregates account balances, total invested value, current portfolio value, P/L, and recent orders."
    )
    public DashboardResponse getDashboardSummary(
            @Parameter(description = "User unique identifier (UUID)")
            @PathVariable UUID userId) {
        return dashboardService.getDashboardSummary(userId);
    }

    @GetMapping("/{userId}/recent-orders")
    @Operation(
        summary = "Get user's recent orders",
        description = "Returns the user's most recent orders sorted chronologically."
    )
    public List<RecentOrderResponse> getRecentOrders(
            @Parameter(description = "User unique identifier (UUID)")
            @PathVariable UUID userId,
            @Parameter(description = "Maximum number of recent orders to return")
            @RequestParam(defaultValue = "10") int limit) {
        return dashboardService.getRecentOrders(userId, limit);
    }

    @GetMapping("/market-summary")
    @Operation(
        summary = "Get market summary",
        description = "Returns overall market status, stock count, and quotes with daily change indicators."
    )
    public MarketSummaryResponse getMarketSummary() {
        return dashboardService.getMarketSummary();
    }
}

