package com.stocktrading.portfolio.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 2: Domain Model & OOP Tests")
class ModelClassesTest {

    @Test
    @DisplayName("HoldingKey: Normalizes stock symbol to uppercase and computes equal hashcodes")
    void testHoldingKey() {
        HoldingKey key1 = new HoldingKey(101L, "tcs");
        HoldingKey key2 = new HoldingKey(101L, "TCS");
        HoldingKey key3 = new HoldingKey(102L, "TCS");

        assertEquals(key1, key2);
        assertEquals(key1.hashCode(), key2.hashCode());
        assertNotEquals(key1, key3);
        assertEquals("TCS", key1.stockSymbol());

        assertThrows(NullPointerException.class, () -> new HoldingKey(null, "INFY"));
        assertThrows(NullPointerException.class, () -> new HoldingKey(101L, null));
        assertThrows(IllegalArgumentException.class, () -> new HoldingKey(101L, "   "));
    }

    @Test
    @DisplayName("Holding: Validates encapsulation and rejects negative quantity or price")
    void testHoldingEncapsulationAndValidation() {
        BigDecimal price = new BigDecimal("1500.50");
        Holding holding = new Holding(101L, "INFY", 10, price);

        assertEquals(101L, holding.getUserId());
        assertEquals("INFY", holding.getStockSymbol());
        assertEquals(10, holding.getQuantity());
        assertEquals(price, holding.getAveragePurchasePrice());
        assertNotNull(holding.getCreatedAt());
        assertNotNull(holding.getUpdatedAt());

        // Test mutator updates timestamp and enforces invariants
        holding.setQuantity(25);
        assertEquals(25, holding.getQuantity());

        assertThrows(IllegalArgumentException.class, () -> holding.setQuantity(-5));
        assertThrows(IllegalArgumentException.class, () -> holding.setAveragePurchasePrice(new BigDecimal("-10.0")));
        assertThrows(IllegalArgumentException.class, () -> new Holding(101L, "INFY", -1, price));
        assertThrows(IllegalArgumentException.class, () -> new Holding(101L, "INFY", 10, new BigDecimal("-5.00")));
    }

    @Test
    @DisplayName("Portfolio: Ensures unmodifiable holdings list and metrics encapsulation")
    void testPortfolioModel() {
        Holding h = new Holding(101L, "RELIANCE", 5, new BigDecimal("2800.00"));
        Portfolio portfolio = new Portfolio(
                101L,
                List.of(h),
                new BigDecimal("14000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("7.14")
        );

        assertEquals(101L, portfolio.getUserId());
        assertEquals(1, portfolio.getHoldings().size());
        assertEquals(new BigDecimal("14000.00"), portfolio.getTotalInvestedValue());
        assertEquals(new BigDecimal("15000.00"), portfolio.getTotalCurrentValue());
        assertEquals(new BigDecimal("1000.00"), portfolio.getTotalUnrealizedProfitLoss());
        assertEquals(new BigDecimal("7.14"), portfolio.getTotalReturnPercentage());

        // Verify defensive unmodifiable list
        List<Holding> holdings = portfolio.getHoldings();
        assertThrows(UnsupportedOperationException.class, () -> holdings.add(new Holding(101L, "TCS", 1, BigDecimal.TEN)));

        // Empty portfolio factory
        Portfolio emptyPortfolio = Portfolio.empty(102L);
        assertEquals(102L, emptyPortfolio.getUserId());
        assertTrue(emptyPortfolio.getHoldings().isEmpty());
        assertEquals(BigDecimal.ZERO, emptyPortfolio.getTotalInvestedValue());
    }

    @Test
    @DisplayName("WatchlistItem: Verifies equality based on userId and stockSymbol")
    void testWatchlistItem() {
        WatchlistItem item1 = new WatchlistItem(1L, 101L, "tcs");
        WatchlistItem item2 = new WatchlistItem(2L, 101L, "TCS");
        WatchlistItem item3 = new WatchlistItem(3L, 102L, "TCS");

        assertEquals(item1, item2, "Watchlist items with same userId and symbol should be considered equal");
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotEquals(item1, item3, "Different users can track the same stock independently");
        assertEquals("TCS", item1.getStockSymbol());
        assertNotNull(item1.getAddedAt());
    }
}
