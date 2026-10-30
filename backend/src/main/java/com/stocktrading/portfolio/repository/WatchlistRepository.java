package com.stocktrading.portfolio.repository;

import com.stocktrading.portfolio.model.WatchlistItem;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory repository for storing and managing user watchlists.
 *
 * Uses thread-safe data structures (ConcurrentHashMap and CopyOnWriteArrayList)
 * with an AtomicLong sequence generator for item IDs.
 */
@Repository
public class WatchlistRepository {

    private final Map<Long, List<WatchlistItem>> userWatchlists = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    /**
     * Saves a watchlist item. Generates an ID if not present.
     */
    public WatchlistItem save(WatchlistItem item) {
        Objects.requireNonNull(item, "WatchlistItem cannot be null");
        Objects.requireNonNull(item.getUserId(), "userId cannot be null");
        Objects.requireNonNull(item.getStockSymbol(), "stockSymbol cannot be null");

        if (item.getId() == null) {
            item.setId(idSequence.getAndIncrement());
        }

        List<WatchlistItem> items = userWatchlists.computeIfAbsent(
                item.getUserId(),
                k -> new CopyOnWriteArrayList<>()
        );

        // Remove if existing version is present (update semantics)
        items.removeIf(existing -> existing.getStockSymbol().equalsIgnoreCase(item.getStockSymbol()));
        items.add(item);

        return item;
    }

    /**
     * Retrieves all watchlist items for a specific user.
     */
    public List<WatchlistItem> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<WatchlistItem> items = userWatchlists.get(userId);
        if (items == null) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * Finds a specific watchlist item by userId and stockSymbol.
     */
    public Optional<WatchlistItem> findByUserIdAndStockSymbol(Long userId, String stockSymbol) {
        if (userId == null || stockSymbol == null || stockSymbol.isBlank()) {
            return Optional.empty();
        }
        List<WatchlistItem> items = userWatchlists.get(userId);
        if (items == null) {
            return Optional.empty();
        }
        String normalizedSymbol = stockSymbol.trim();
        return items.stream()
                .filter(item -> item.getStockSymbol().equalsIgnoreCase(normalizedSymbol))
                .findFirst();
    }

    /**
     * Checks if a user already has a specific stock in their watchlist.
     */
    public boolean existsByUserIdAndStockSymbol(Long userId, String stockSymbol) {
        return findByUserIdAndStockSymbol(userId, stockSymbol).isPresent();
    }

    /**
     * Removes a stock from a user's watchlist.
     * @return true if an item was removed, false otherwise
     */
    public boolean deleteByUserIdAndStockSymbol(Long userId, String stockSymbol) {
        if (userId == null || stockSymbol == null || stockSymbol.isBlank()) {
            return false;
        }
        List<WatchlistItem> items = userWatchlists.get(userId);
        if (items == null) {
            return false;
        }
        String normalizedSymbol = stockSymbol.trim();
        return items.removeIf(item -> item.getStockSymbol().equalsIgnoreCase(normalizedSymbol));
    }

    /**
     * Clears all watchlists from memory (for testing purposes).
     */
    public void clear() {
        userWatchlists.clear();
        idSequence.set(1);
    }
}
