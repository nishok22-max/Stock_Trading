package com.stocktrading.portfolio.controller;

import com.stocktrading.portfolio.dto.HoldingResponse;
import com.stocktrading.portfolio.dto.PortfolioPerformanceResponse;
import com.stocktrading.portfolio.dto.PortfolioResponse;
import com.stocktrading.portfolio.integration.ExecutedTrade;
import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.service.HoldingService;
import com.stocktrading.portfolio.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for portfolio queries, holding valuations, and trade execution updates.
 */
@RestController
@RequestMapping("/api/portfolio")
@Tag(name = "Portfolio Management", description = "Endpoints for portfolio overview, individual holdings, and performance metrics")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final HoldingService holdingService;

    public PortfolioController(PortfolioService portfolioService, HoldingService holdingService) {
        this.portfolioService = portfolioService;
        this.holdingService = holdingService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user portfolio", description = "Retrieves total invested value, current market valuation, unrealized P&L, return %, and holding allocations.")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable Long userId) {
        PortfolioResponse response = portfolioService.getPortfolio(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{userId}/holdings")
    @Operation(summary = "Get user holdings", description = "Retrieves all stock holdings for a user with purchase price, market price, and allocation %.")
    public ResponseEntity<List<HoldingResponse>> getHoldings(@PathVariable Long userId) {
        List<HoldingResponse> holdings = portfolioService.getHoldings(userId);
        return ResponseEntity.ok(holdings);
    }

    @GetMapping("/{userId}/performance")
    @Operation(summary = "Get portfolio performance", description = "Retrieves high-level performance indicators, top gainers, top losers, and return percentages.")
    public ResponseEntity<PortfolioPerformanceResponse> getPerformance(@PathVariable Long userId) {
        PortfolioPerformanceResponse performance = portfolioService.getPerformance(userId);
        return ResponseEntity.ok(performance);
    }

    @PostMapping("/trades/execute")
    @Operation(summary = "Receive executed trade", description = "Integration boundary endpoint to receive executed buy/sell trades from Member 5.")
    public ResponseEntity<Map<String, Object>> executeTrade(@Valid @RequestBody ExecutedTrade trade) {
        Holding updatedHolding = holdingService.processTrade(trade);
        if (updatedHolding == null) {
            return ResponseEntity.ok(Map.of(
                    "message", "Position completely closed. Stock holding removed.",
                    "stockSymbol", trade.getStockSymbol(),
                    "side", trade.getSide()
            ));
        }
        return ResponseEntity.ok(Map.of(
                "message", "Trade processed successfully and holding updated.",
                "holding", updatedHolding
        ));
    }
}
