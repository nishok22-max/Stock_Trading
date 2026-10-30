package com.stocktrading.portfolio.service;

import com.stocktrading.portfolio.dto.HoldingResponse;
import com.stocktrading.portfolio.dto.PortfolioResponse;
import com.stocktrading.portfolio.integration.StockPriceProvider;
import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.repository.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9: PortfolioService Valuation & Performance Tests")
class PortfolioServiceTest {

    @Mock
    private StockPriceProvider stockPriceProvider;

    private HoldingRepository holdingRepository;
    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        holdingRepository = new HoldingRepository();
        portfolioService = new PortfolioService(holdingRepository, stockPriceProvider);
    }

    @Test
    @DisplayName("1 & 2. Invested & Current Value Calculation: Computes sums across multiple positions")
    void testInvestedAndCurrentValueCalculation() {
        Long userId = 101L;

        // Holding 1: 10 TCS @ 3000 (Invested = 30,000)
        holdingRepository.save(new Holding(userId, "TCS", 10, new BigDecimal("3000.00")));
        // Holding 2: 20 INFY @ 1500 (Invested = 30,000)
        holdingRepository.save(new Holding(userId, "INFY", 20, new BigDecimal("1500.00")));

        when(stockPriceProvider.getCurrentPrice("TCS")).thenReturn(new BigDecimal("3500.00")); // Current = 35,000
        when(stockPriceProvider.getCurrentPrice("INFY")).thenReturn(new BigDecimal("1600.00")); // Current = 32,000

        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Total Invested = 30,000 + 30,000 = 60,000.00
        assertEquals(new BigDecimal("60000.00"), response.getTotalInvestedValue());
        // Total Current = 35,000 + 32,000 = 67,000.00
        assertEquals(new BigDecimal("67000.00"), response.getTotalCurrentValue());
    }

    @Test
    @DisplayName("3. Positive Unrealized P&L: Accurately reflects profit and return percentage")
    void testPositiveUnrealizedPnL() {
        Long userId = 101L;

        // Buy 100 shares @ 100 = Invested 10,000
        holdingRepository.save(new Holding(userId, "RELIANCE", 100, new BigDecimal("100.00")));
        // Current price is 150 = Current 15,000
        when(stockPriceProvider.getCurrentPrice("RELIANCE")).thenReturn(new BigDecimal("150.00"));

        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Unrealized P&L = 15,000 - 10,000 = +5,000.00
        assertEquals(new BigDecimal("5000.00"), response.getTotalUnrealizedProfitLoss());
        // Return % = (5,000 / 10,000) * 100 = 50.00%
        assertEquals(new BigDecimal("50.00"), response.getTotalReturnPercentage());
    }

    @Test
    @DisplayName("4. Negative Unrealized P&L: Accurately reflects loss and negative return percentage")
    void testNegativeUnrealizedPnL() {
        Long userId = 101L;

        // Buy 100 shares @ 200 = Invested 20,000
        holdingRepository.save(new Holding(userId, "TATAMOTORS", 100, new BigDecimal("200.00")));
        // Current price drops to 160 = Current 16,000
        when(stockPriceProvider.getCurrentPrice("TATAMOTORS")).thenReturn(new BigDecimal("160.00"));

        PortfolioResponse response = portfolioService.getPortfolio(userId);

        // Unrealized P&L = 16,000 - 20,000 = -4,000.00
        assertEquals(new BigDecimal("-4000.00"), response.getTotalUnrealizedProfitLoss());
        // Return % = (-4,000 / 20,000) * 100 = -20.00%
        assertEquals(new BigDecimal("-20.00"), response.getTotalReturnPercentage());
    }

    @Test
    @DisplayName("5. Holding Allocation: Correctly calculates percentage share of total portfolio")
    void testHoldingAllocationCalculation() {
        Long userId = 101L;

        // Stock A current value = 6,000 (60%)
        holdingRepository.save(new Holding(userId, "STOCKA", 10, new BigDecimal("500.00")));
        when(stockPriceProvider.getCurrentPrice("STOCKA")).thenReturn(new BigDecimal("600.00"));

        // Stock B current value = 4,000 (40%)
        holdingRepository.save(new Holding(userId, "STOCKB", 10, new BigDecimal("350.00")));
        when(stockPriceProvider.getCurrentPrice("STOCKB")).thenReturn(new BigDecimal("400.00"));

        PortfolioResponse response = portfolioService.getPortfolio(userId);
        List<HoldingResponse> holdings = response.getHoldings();

        HoldingResponse stockA = holdings.stream().filter(h -> h.getStockSymbol().equals("STOCKA")).findFirst().orElseThrow();
        HoldingResponse stockB = holdings.stream().filter(h -> h.getStockSymbol().equals("STOCKB")).findFirst().orElseThrow();

        assertEquals(new BigDecimal("60.00"), stockA.getAllocationPercentage());
        assertEquals(new BigDecimal("40.00"), stockB.getAllocationPercentage());
    }

    @Test
    @DisplayName("6. Zero-Investment Handling: Returns 0.00 values without ArithmeticException")
    void testZeroInvestmentHandling() {
        Long userId = 999L; // User with no holdings

        PortfolioResponse response = portfolioService.getPortfolio(userId);

        assertEquals(0, response.getTotalHoldingsCount());
        assertEquals(new BigDecimal("0.00"), response.getTotalInvestedValue());
        assertEquals(new BigDecimal("0.00"), response.getTotalCurrentValue());
        assertEquals(new BigDecimal("0.00"), response.getTotalUnrealizedProfitLoss());
        assertEquals(new BigDecimal("0.00"), response.getTotalReturnPercentage());
    }
}
