package com.stocktrading.portfolio.service;

import com.stocktrading.portfolio.exception.BadRequestException;
import com.stocktrading.portfolio.exception.ResourceNotFoundException;
import com.stocktrading.portfolio.integration.StockPriceProvider;
import com.stocktrading.portfolio.model.WatchlistItem;
import com.stocktrading.portfolio.repository.WatchlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9: WatchlistService Unit Tests")
class WatchlistServiceTest {

    @Mock
    private StockPriceProvider stockPriceProvider;

    private WatchlistRepository watchlistRepository;
    private WatchlistService watchlistService;

    @BeforeEach
    void setUp() {
        watchlistRepository = new WatchlistRepository();
        watchlistService = new WatchlistService(watchlistRepository, stockPriceProvider);
    }

    @Test
    @DisplayName("1. Add Stock: Successfully adds recognized stock ticker to user's watchlist")
    void testAddStockToWatchlist() {
        Long userId = 101L;
        String symbol = "TCS";

        when(stockPriceProvider.isValidStock("TCS")).thenReturn(true);

        WatchlistItem item = watchlistService.addToWatchlist(userId, symbol);

        assertNotNull(item);
        assertEquals(userId, item.getUserId());
        assertEquals("TCS", item.getStockSymbol());
        assertNotNull(item.getId());
        assertNotNull(item.getAddedAt());
    }

    @Test
    @DisplayName("2. Duplicate Stock Rejection: Throws BadRequestException when user adds existing ticker")
    void testDuplicateStockRejection() {
        Long userId = 101L;
        String symbol = "INFY";

        when(stockPriceProvider.isValidStock("INFY")).thenReturn(true);

        watchlistService.addToWatchlist(userId, symbol);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                watchlistService.addToWatchlist(userId, "infy") // case-insensitive check
        );

        assertTrue(ex.getMessage().contains("already in user 101's watchlist"));
    }

    @Test
    @DisplayName("3. Remove Stock: Successfully removes tracked stock from watchlist")
    void testRemoveStock() {
        Long userId = 101L;
        String symbol = "RELIANCE";

        when(stockPriceProvider.isValidStock("RELIANCE")).thenReturn(true);
        watchlistService.addToWatchlist(userId, symbol);

        boolean removed = watchlistService.removeFromWatchlist(userId, symbol);

        assertTrue(removed);
        assertTrue(watchlistService.getUserWatchlist(userId).isEmpty());
    }

    @Test
    @DisplayName("4. Remove Nonexistent Stock: Throws ResourceNotFoundException")
    void testRemoveNonexistentStock() {
        Long userId = 101L;

        assertThrows(ResourceNotFoundException.class, () ->
                watchlistService.removeFromWatchlist(userId, "NONEXISTENT")
        );
    }

    @Test
    @DisplayName("5. Multi-User Isolation: Different users can track same stock independently")
    void testRetrieveWatchlistForDifferentUsers() {
        Long user1 = 101L;
        Long user2 = 102L;

        when(stockPriceProvider.isValidStock("TCS")).thenReturn(true);
        when(stockPriceProvider.isValidStock("AAPL")).thenReturn(true);

        // User 1 tracks TCS and AAPL
        watchlistService.addToWatchlist(user1, "TCS");
        watchlistService.addToWatchlist(user1, "AAPL");

        // User 2 tracks TCS only
        watchlistService.addToWatchlist(user2, "TCS");

        List<WatchlistItem> user1List = watchlistService.getUserWatchlist(user1);
        List<WatchlistItem> user2List = watchlistService.getUserWatchlist(user2);

        assertEquals(2, user1List.size());
        assertEquals(1, user2List.size());
        assertEquals("TCS", user2List.get(0).getStockSymbol());
    }

    @Test
    @DisplayName("Validation: Rejects invalid stock tickers not recognized by StockPriceProvider")
    void testInvalidStockTickerRejection() {
        Long userId = 101L;
        when(stockPriceProvider.isValidStock("INVALID_SYMBOL")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                watchlistService.addToWatchlist(userId, "INVALID_SYMBOL")
        );

        assertTrue(ex.getMessage().contains("Invalid stock symbol"));
    }
}
