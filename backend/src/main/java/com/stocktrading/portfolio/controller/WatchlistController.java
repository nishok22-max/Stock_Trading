package com.stocktrading.portfolio.controller;

import com.stocktrading.portfolio.dto.WatchlistRequest;
import com.stocktrading.portfolio.dto.WatchlistResponse;
import com.stocktrading.portfolio.model.WatchlistItem;
import com.stocktrading.portfolio.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user watchlists (favourite stock tickers).
 */
@RestController
@RequestMapping("/api/watchlist")
@Tag(name = "Watchlist Management", description = "Endpoints for retrieving, adding, and removing tracked stocks")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user watchlist", description = "Retrieves all stocks in the user's watchlist with current market prices.")
    public ResponseEntity<List<WatchlistResponse>> getWatchlist(@PathVariable Long userId) {
        List<WatchlistResponse> watchlist = watchlistService.getUserWatchlistWithPrices(userId);
        return ResponseEntity.ok(watchlist);
    }

    @PostMapping
    @Operation(summary = "Add stock to watchlist", description = "Adds a valid stock ticker to the user's watchlist. Prevents duplicate tickers.")
    public ResponseEntity<WatchlistItem> addToWatchlist(@Valid @RequestBody WatchlistRequest request) {
        WatchlistItem savedItem = watchlistService.addToWatchlist(request.getUserId(), request.getStockSymbol());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedItem);
    }

    @DeleteMapping("/{userId}/{symbol}")
    @Operation(summary = "Remove stock from watchlist", description = "Removes a specific stock ticker from the user's watchlist.")
    public ResponseEntity<Map<String, String>> removeFromWatchlist(@PathVariable Long userId, @PathVariable String symbol) {
        watchlistService.removeFromWatchlist(userId, symbol);
        return ResponseEntity.ok(Map.of(
                "message", "Stock '" + symbol.toUpperCase() + "' removed from watchlist successfully.",
                "userId", String.valueOf(userId),
                "stockSymbol", symbol.toUpperCase()
        ));
    }
}
