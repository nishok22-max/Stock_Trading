package com.stocktrading.portfolio.service;

import com.stocktrading.portfolio.dto.WatchlistResponse;
import com.stocktrading.portfolio.exception.BadRequestException;
import com.stocktrading.portfolio.exception.ResourceNotFoundException;
import com.stocktrading.portfolio.integration.StockPriceProvider;
import com.stocktrading.portfolio.model.WatchlistItem;
import com.stocktrading.portfolio.repository.WatchlistRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service managing user watchlists, symbol validations, and duplicate prevention.
 */
@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final StockPriceProvider stockPriceProvider;

    public WatchlistService(WatchlistRepository watchlistRepository, StockPriceProvider stockPriceProvider) {
        this.watchlistRepository = watchlistRepository;
        this.stockPriceProvider = stockPriceProvider;
    }

    /**
     * Adds a stock to the user's watchlist.
     * Validates that the symbol is recognized by Member 2's Stock module,
     * and ensures no duplicate entries exist for this user.
     */
    public WatchlistItem addToWatchlist(Long userId, String stockSymbol) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.isBlank()) {
            throw new BadRequestException("Stock symbol cannot be blank");
        }

        String normalizedSymbol = stockSymbol.trim().toUpperCase();

        // Validate ticker against stock integration interface
        if (!stockPriceProvider.isValidStock(normalizedSymbol)) {
            throw new BadRequestException("Invalid stock symbol: '" + stockSymbol + "'. Not found in market catalog.");
        }

        // Prevent duplicate stock entry for the same user
        if (watchlistRepository.existsByUserIdAndStockSymbol(userId, normalizedSymbol)) {
            throw new BadRequestException("Stock '" + normalizedSymbol + "' is already in user " + userId + "'s watchlist.");
        }

        WatchlistItem item = new WatchlistItem(null, userId, normalizedSymbol);
        return watchlistRepository.save(item);
    }

    /**
     * Removes a stock from the user's watchlist.
     */
    public boolean removeFromWatchlist(Long userId, String stockSymbol) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.isBlank()) {
            throw new BadRequestException("Stock symbol cannot be blank");
        }

        String normalizedSymbol = stockSymbol.trim().toUpperCase();
        boolean removed = watchlistRepository.deleteByUserIdAndStockSymbol(userId, normalizedSymbol);
        if (!removed) {
            throw new ResourceNotFoundException(
                    "Stock '" + normalizedSymbol + "' not found in user " + userId + "'s watchlist."
            );
        }
        return true;
    }

    /**
     * Retrieves all raw watchlist items for a given user.
     */
    public List<WatchlistItem> getUserWatchlist(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return watchlistRepository.findByUserId(userId);
    }

    /**
     * Retrieves enriched watchlist items including current market prices.
     */
    public List<WatchlistResponse> getUserWatchlistWithPrices(Long userId) {
        List<WatchlistItem> items = getUserWatchlist(userId);
        List<WatchlistResponse> responses = new ArrayList<>(items.size());

        for (WatchlistItem item : items) {
            BigDecimal currentPrice = BigDecimal.ZERO;
            try {
                currentPrice = stockPriceProvider.getCurrentPrice(item.getStockSymbol());
            } catch (Exception ignored) {
                // Keep default if provider fails
            }

            responses.add(new WatchlistResponse(
                    item.getId(),
                    item.getUserId(),
                    item.getStockSymbol(),
                    currentPrice,
                    item.getAddedAt()
            ));
        }

        return responses;
    }
}
