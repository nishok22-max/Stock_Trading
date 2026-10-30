package com.stocktrading.portfolio.service;

import com.stocktrading.portfolio.exception.BadRequestException;
import com.stocktrading.portfolio.exception.ResourceNotFoundException;
import com.stocktrading.portfolio.integration.ExecutedTrade;
import com.stocktrading.portfolio.integration.TradeSide;
import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.repository.HoldingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service responsible for managing user holdings and executing buy/sell trade updates.
 *
 * Implements weighted average purchase price calculations, inventory balance validation,
 * and zero-quantity position removal.
 */
@Service
public class HoldingService {

    private static final int CALCULATION_SCALE = 4;
    private final HoldingRepository holdingRepository;

    public HoldingService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    /**
     * Entry point for processing an executed trade received across the Member 5 integration boundary.
     *
     * @param trade the executed trade DTO
     * @return updated Holding, or null if position was completely closed (quantity reached 0)
     */
    public Holding processTrade(ExecutedTrade trade) {
        Objects.requireNonNull(trade, "ExecutedTrade cannot be null");
        validateTrade(trade);

        if (trade.getSide() == TradeSide.BUY) {
            return processBuyTrade(trade.getUserId(), trade.getStockSymbol(), trade.getQuantity(), trade.getExecutionPrice());
        } else if (trade.getSide() == TradeSide.SELL) {
            return processSellTrade(trade.getUserId(), trade.getStockSymbol(), trade.getQuantity(), trade.getExecutionPrice());
        } else {
            throw new BadRequestException("Unsupported trade side: " + trade.getSide());
        }
    }

    /**
     * Processes a BUY trade.
     * If the holding already exists, calculates the new weighted average purchase price.
     * If the holding does not exist, creates a new consolidated position.
     *
     * Weighted Average Purchase Price Formula:
     * New Average Price = ((Old Quantity × Old Average Price) + (New Quantity × New Purchase Price)) / Total Quantity
     */
    public Holding processBuyTrade(Long userId, String stockSymbol, int quantity, BigDecimal purchasePrice) {
        validateInputs(userId, stockSymbol, quantity, purchasePrice);
        String symbol = stockSymbol.trim().toUpperCase();

        Optional<Holding> existingOpt = holdingRepository.findByUserIdAndStockSymbol(userId, symbol);

        if (existingOpt.isEmpty()) {
            // First buy: purchase price becomes the initial average purchase price
            BigDecimal roundedPrice = purchasePrice.setScale(CALCULATION_SCALE, RoundingMode.HALF_UP);
            Holding newHolding = new Holding(userId, symbol, quantity, roundedPrice);
            return holdingRepository.save(newHolding);
        }

        Holding holding = existingOpt.get();
        int oldQuantity = holding.getQuantity();
        BigDecimal oldAvgPrice = holding.getAveragePurchasePrice();

        int totalQuantity = oldQuantity + quantity;

        // Old Invested = Old Quantity * Old Average Price
        BigDecimal oldTotalInvested = oldAvgPrice.multiply(BigDecimal.valueOf(oldQuantity));

        // New Buy Invested = New Quantity * New Purchase Price
        BigDecimal newBuyInvested = purchasePrice.multiply(BigDecimal.valueOf(quantity));

        // Total Invested = Old Invested + New Buy Invested
        BigDecimal totalInvested = oldTotalInvested.add(newBuyInvested);

        // Weighted Average = Total Invested / Total Quantity
        BigDecimal newAveragePrice = totalInvested.divide(
                BigDecimal.valueOf(totalQuantity),
                CALCULATION_SCALE,
                RoundingMode.HALF_UP
        );

        holding.setQuantity(totalQuantity);
        holding.setAveragePurchasePrice(newAveragePrice);

        return holdingRepository.save(holding);
    }

    /**
     * Processes a SELL trade.
     * - Prevents selling more shares than the user owns.
     * - Removes the holding completely if the remaining quantity drops to zero.
     * - Keeps the average purchase price unchanged for remaining shares after a partial sell.
     */
    public Holding processSellTrade(Long userId, String stockSymbol, int quantity, BigDecimal sellPrice) {
        validateInputs(userId, stockSymbol, quantity, sellPrice);
        String symbol = stockSymbol.trim().toUpperCase();

        Holding holding = holdingRepository.findByUserIdAndStockSymbol(userId, symbol)
                .orElseThrow(() -> new BadRequestException(
                        "Cannot sell stock " + symbol + ": User " + userId + " does not own any shares."
                ));

        int currentQuantity = holding.getQuantity();

        if (quantity > currentQuantity) {
            throw new BadRequestException(
                    "Cannot sell " + quantity + " shares of " + symbol +
                    ". User " + userId + " only owns " + currentQuantity + " shares."
            );
        }

        if (quantity == currentQuantity) {
            // Complete sell: position is closed, remove holding from repository
            holdingRepository.delete(holding);
            return null;
        }

        // Partial sell: reduce quantity, retain original average purchase price
        int remainingQuantity = currentQuantity - quantity;
        holding.setQuantity(remainingQuantity);

        return holdingRepository.save(holding);
    }

    /**
     * Retrieves all holdings for a specific user.
     */
    public List<Holding> getUserHoldings(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        return holdingRepository.findByUserId(userId);
    }

    /**
     * Retrieves a single holding by user ID and stock symbol.
     */
    public Holding getHolding(Long userId, String stockSymbol) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.isBlank()) {
            throw new BadRequestException("Stock symbol cannot be blank");
        }
        return holdingRepository.findByUserIdAndStockSymbol(userId, stockSymbol.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Holding not found for user " + userId + " and stock " + stockSymbol
                ));
    }

    private void validateTrade(ExecutedTrade trade) {
        validateInputs(trade.getUserId(), trade.getStockSymbol(), trade.getQuantity(), trade.getExecutionPrice());
        if (trade.getSide() == null) {
            throw new BadRequestException("Trade side (BUY/SELL) cannot be null");
        }
    }

    private void validateInputs(Long userId, String stockSymbol, Integer quantity, BigDecimal price) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }
        if (stockSymbol == null || stockSymbol.isBlank()) {
            throw new BadRequestException("Stock symbol cannot be blank");
        }
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Trade quantity must be greater than zero. Received: " + quantity);
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Trade price must be greater than zero. Received: " + price);
        }
    }
}
