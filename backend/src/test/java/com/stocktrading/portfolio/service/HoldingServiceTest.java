package com.stocktrading.portfolio.service;

import com.stocktrading.portfolio.exception.BadRequestException;
import com.stocktrading.portfolio.exception.ResourceNotFoundException;
import com.stocktrading.portfolio.integration.ExecutedTrade;
import com.stocktrading.portfolio.integration.TradeSide;
import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.repository.HoldingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 9: HoldingService Unit Tests")
class HoldingServiceTest {

    private HoldingRepository holdingRepository;
    private HoldingService holdingService;

    @BeforeEach
    void setUp() {
        holdingRepository = new HoldingRepository();
        holdingService = new HoldingService(holdingRepository);
    }

    @Test
    @DisplayName("1. First Buy: Creates new consolidated holding with initial purchase price")
    void testFirstBuy() {
        Long userId = 101L;
        String symbol = "TCS";
        int quantity = 10;
        BigDecimal price = new BigDecimal("3500.00");

        Holding holding = holdingService.processBuyTrade(userId, symbol, quantity, price);

        assertNotNull(holding);
        assertEquals(userId, holding.getUserId());
        assertEquals(symbol, holding.getStockSymbol());
        assertEquals(10, holding.getQuantity());
        assertEquals(new BigDecimal("3500.0000"), holding.getAveragePurchasePrice());
    }

    @Test
    @DisplayName("2 & 3. Second Buy: Accurately computes weighted average purchase price")
    void testSecondBuyWeightedAverageCalculation() {
        Long userId = 101L;
        String symbol = "INFY";

        // First buy: 10 shares @ 1000 = 10,000
        holdingService.processBuyTrade(userId, symbol, 10, new BigDecimal("1000.00"));

        // Second buy: 20 shares @ 1600 = 32,000
        // Total invested = 42,000, Total quantity = 30
        // Expected weighted avg = 42,000 / 30 = 1400.0000
        Holding updated = holdingService.processBuyTrade(userId, symbol, 20, new BigDecimal("1600.00"));

        assertEquals(30, updated.getQuantity());
        assertEquals(new BigDecimal("1400.0000"), updated.getAveragePurchasePrice());
    }

    @Test
    @DisplayName("4. Partial Sell: Decreases quantity while keeping average purchase price unchanged")
    void testPartialSell() {
        Long userId = 101L;
        String symbol = "RELIANCE";

        // Buy 50 shares @ 2500
        holdingService.processBuyTrade(userId, symbol, 50, new BigDecimal("2500.00"));

        // Partial sell 20 shares @ 2700
        Holding updated = holdingService.processSellTrade(userId, symbol, 20, new BigDecimal("2700.00"));

        assertNotNull(updated);
        assertEquals(30, updated.getQuantity());
        // Price must remain unchanged for remaining shares
        assertEquals(new BigDecimal("2500.0000"), updated.getAveragePurchasePrice());
    }

    @Test
    @DisplayName("5. Complete Sell: Removes holding completely when quantity becomes zero")
    void testCompleteSell() {
        Long userId = 101L;
        String symbol = "HDFCBANK";

        // Buy 15 shares @ 1500
        holdingService.processBuyTrade(userId, symbol, 15, new BigDecimal("1500.00"));

        // Sell all 15 shares
        Holding result = holdingService.processSellTrade(userId, symbol, 15, new BigDecimal("1600.00"));

        assertNull(result, "Service should return null when position is completely closed");
        assertTrue(holdingRepository.findByUserIdAndStockSymbol(userId, symbol).isEmpty(),
                "Holding must be removed from repository when quantity drops to zero");
    }

    @Test
    @DisplayName("6. Sell Validation: Prevents selling more shares than the user owns")
    void testSellMoreThanOwned() {
        Long userId = 101L;
        String symbol = "WIPRO";

        holdingService.processBuyTrade(userId, symbol, 10, new BigDecimal("400.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                holdingService.processSellTrade(userId, symbol, 15, new BigDecimal("450.00"))
        );

        assertTrue(ex.getMessage().contains("Cannot sell 15 shares"));
    }

    @Test
    @DisplayName("7. Validation: Rejects non-positive or zero trade quantity")
    void testInvalidQuantity() {
        Long userId = 101L;
        String symbol = "TCS";
        BigDecimal price = new BigDecimal("3500.00");

        assertThrows(BadRequestException.class, () ->
                holdingService.processBuyTrade(userId, symbol, 0, price)
        );
        assertThrows(BadRequestException.class, () ->
                holdingService.processBuyTrade(userId, symbol, -5, price)
        );
    }

    @Test
    @DisplayName("8. Validation: Rejects null, zero, or negative trade price")
    void testInvalidPrice() {
        Long userId = 101L;
        String symbol = "TCS";

        assertThrows(BadRequestException.class, () ->
                holdingService.processBuyTrade(userId, symbol, 10, BigDecimal.ZERO)
        );
        assertThrows(BadRequestException.class, () ->
                holdingService.processBuyTrade(userId, symbol, 10, new BigDecimal("-100.00"))
        );
        assertThrows(BadRequestException.class, () ->
                holdingService.processBuyTrade(userId, symbol, 10, null)
        );
    }

    @Test
    @DisplayName("Process ExecutedTrade DTO: Routes BUY and SELL trades cleanly")
    void testProcessExecutedTrade() {
        Long userId = 102L;

        ExecutedTrade buyTrade = new ExecutedTrade(
                1L, userId, "AAPL", TradeSide.BUY, 10, new BigDecimal("200.00"), null
        );
        Holding buyResult = holdingService.processTrade(buyTrade);
        assertEquals(10, buyResult.getQuantity());

        ExecutedTrade sellTrade = new ExecutedTrade(
                2L, userId, "AAPL", TradeSide.SELL, 5, new BigDecimal("220.00"), null
        );
        Holding sellResult = holdingService.processTrade(sellTrade);
        assertEquals(5, sellResult.getQuantity());
    }
}
