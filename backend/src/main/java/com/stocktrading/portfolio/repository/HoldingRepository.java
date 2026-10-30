package com.stocktrading.portfolio.repository;

import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.model.HoldingKey;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for storing and managing user stock holdings.
 *
 * Utilizes a ConcurrentHashMap keyed by the composite HoldingKey (userId, stockSymbol)
 * ensuring thread-safe operations without requiring an external database.
 */
@Repository
public class HoldingRepository {

    private final Map<HoldingKey, Holding> holdingStore = new ConcurrentHashMap<>();

    /**
     * Saves or updates a holding in the in-memory store.
     */
    public Holding save(Holding holding) {
        Objects.requireNonNull(holding, "Holding cannot be null");
        Objects.requireNonNull(holding.getUserId(), "Holding userId cannot be null");
        Objects.requireNonNull(holding.getStockSymbol(), "Holding stockSymbol cannot be null");

        HoldingKey key = holding.getKey();
        holdingStore.put(key, holding);
        return holding;
    }

    /**
     * Finds a single holding by user ID and stock symbol.
     */
    public Optional<Holding> findByUserIdAndStockSymbol(Long userId, String stockSymbol) {
        if (userId == null || stockSymbol == null || stockSymbol.isBlank()) {
            return Optional.empty();
        }
        HoldingKey key = new HoldingKey(userId, stockSymbol);
        return Optional.ofNullable(holdingStore.get(key));
    }

    /**
     * Retrieves all active holdings belonging to a specific user.
     */
    public List<Holding> findByUserId(Long userId) {
        if (userId == null) {
            return List.of();
        }
        List<Holding> userHoldings = new ArrayList<>();
        for (Map.Entry<HoldingKey, Holding> entry : holdingStore.entrySet()) {
            if (entry.getKey().userId().equals(userId)) {
                userHoldings.add(entry.getValue());
            }
        }
        return userHoldings;
    }

    /**
     * Checks if a holding exists for a given user and stock symbol.
     */
    public boolean existsByUserIdAndStockSymbol(Long userId, String stockSymbol) {
        if (userId == null || stockSymbol == null || stockSymbol.isBlank()) {
            return false;
        }
        return holdingStore.containsKey(new HoldingKey(userId, stockSymbol));
    }

    /**
     * Deletes a holding by user ID and stock symbol.
     */
    public boolean deleteByUserIdAndStockSymbol(Long userId, String stockSymbol) {
        if (userId == null || stockSymbol == null || stockSymbol.isBlank()) {
            return false;
        }
        return holdingStore.remove(new HoldingKey(userId, stockSymbol)) != null;
    }

    /**
     * Deletes a given holding entity.
     */
    public boolean delete(Holding holding) {
        if (holding == null) {
            return false;
        }
        return deleteByUserIdAndStockSymbol(holding.getUserId(), holding.getStockSymbol());
    }

    /**
     * Returns total count of holdings across all users.
     */
    public int count() {
        return holdingStore.size();
    }

    /**
     * Clears all holdings from memory (primarily for unit test isolation).
     */
    public void clear() {
        holdingStore.clear();
    }
}
