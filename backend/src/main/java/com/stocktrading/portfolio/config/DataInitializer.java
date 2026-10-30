package com.stocktrading.portfolio.config;

import com.stocktrading.portfolio.integration.TradeSide;
import com.stocktrading.portfolio.service.HoldingService;
import com.stocktrading.portfolio.service.WatchlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Initializes realistic sample portfolio and watchlist data for demonstration
 * and college project review purposes (default test User ID: 101).
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final HoldingService holdingService;
    private final WatchlistService watchlistService;

    public DataInitializer(HoldingService holdingService, WatchlistService watchlistService) {
        this.holdingService = holdingService;
        this.watchlistService = watchlistService;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing demo portfolio and watchlist data for User 101...");

        Long defaultUserId = 101L;

        // Sample Buy Trades for user 101
        holdingService.processBuyTrade(defaultUserId, "TCS", 10, new BigDecimal("3600.00"));
        holdingService.processBuyTrade(defaultUserId, "TCS", 10, new BigDecimal("3800.00")); // Demonstrates weighted average
        holdingService.processBuyTrade(defaultUserId, "INFY", 25, new BigDecimal("1650.00"));
        holdingService.processBuyTrade(defaultUserId, "RELIANCE", 15, new BigDecimal("2820.00"));
        holdingService.processBuyTrade(defaultUserId, "HDFCBANK", 20, new BigDecimal("1550.00"));

        // Sample Watchlist Items
        watchlistService.addToWatchlist(defaultUserId, "AAPL");
        watchlistService.addToWatchlist(defaultUserId, "TATAMOTORS");
        watchlistService.addToWatchlist(defaultUserId, "NVDA");
        watchlistService.addToWatchlist(defaultUserId, "MSFT");

        log.info("Demo portfolio & watchlist initialized successfully for User 101.");
    }
}
