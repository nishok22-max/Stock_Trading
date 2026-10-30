package com.stocktrading.portfolio.service;

import com.stocktrading.portfolio.dto.HoldingResponse;
import com.stocktrading.portfolio.dto.PortfolioPerformanceResponse;
import com.stocktrading.portfolio.dto.PortfolioResponse;
import com.stocktrading.portfolio.exception.BadRequestException;
import com.stocktrading.portfolio.integration.StockPriceProvider;
import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.model.Portfolio;
import com.stocktrading.portfolio.repository.HoldingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Service orchestrating portfolio-level valuation, holding allocations,
 * and unrealized profit/loss performance analytics.
 *
 * All monetary calculations use BigDecimal with explicit RoundingMode.HALF_UP.
 */
@Service
public class PortfolioService {

    private static final int DISPLAY_SCALE = 2;
    private final HoldingRepository holdingRepository;
    private final StockPriceProvider stockPriceProvider;

    public PortfolioService(HoldingRepository holdingRepository, StockPriceProvider stockPriceProvider) {
        this.holdingRepository = holdingRepository;
        this.stockPriceProvider = stockPriceProvider;
    }

    /**
     * Retrieves the consolidated portfolio response for a user, complete with
     * live market valuations, unrealized P&L, and asset weight allocation percentages.
     */
    public PortfolioResponse getPortfolio(Long userId) {
        if (userId == null) {
            throw new BadRequestException("User ID cannot be null");
        }

        List<Holding> holdings = holdingRepository.findByUserId(userId);
        if (holdings.isEmpty()) {
            return new PortfolioResponse(
                    userId,
                    BigDecimal.ZERO.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    0,
                    List.of()
            );
        }

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrent = BigDecimal.ZERO;

        // First pass: calculate invested and current values for total portfolio summation
        List<HoldingResponseDraft> drafts = new ArrayList<>(holdings.size());
        for (Holding holding : holdings) {
            BigDecimal currentPrice = stockPriceProvider.getCurrentPrice(holding.getStockSymbol());
            BigDecimal qty = BigDecimal.valueOf(holding.getQuantity());

            // Invested Value = Quantity * Average Purchase Price
            BigDecimal investedValue = holding.getAveragePurchasePrice()
                    .multiply(qty)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);

            // Current Value = Quantity * Current Market Price
            BigDecimal currentValue = currentPrice
                    .multiply(qty)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);

            totalInvested = totalInvested.add(investedValue);
            totalCurrent = totalCurrent.add(currentValue);

            drafts.add(new HoldingResponseDraft(holding, currentPrice, investedValue, currentValue));
        }

        // Unrealized P&L = Total Current Value - Total Invested Value
        BigDecimal totalUnrealizedPnL = totalCurrent.subtract(totalInvested)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);

        // Portfolio Return Percentage = (Total P&L / Total Invested Value) * 100
        BigDecimal totalReturnPercentage = calculatePercentage(totalUnrealizedPnL, totalInvested);

        // Second pass: compute individual holding allocation percentage based on total current value
        List<HoldingResponse> holdingResponses = new ArrayList<>(drafts.size());
        for (HoldingResponseDraft draft : drafts) {
            BigDecimal pnl = draft.currentValue.subtract(draft.investedValue)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);

            BigDecimal holdingReturnPct = calculatePercentage(pnl, draft.investedValue);
            BigDecimal allocationPct = calculatePercentage(draft.currentValue, totalCurrent);

            HoldingResponse response = new HoldingResponse(
                    draft.holding.getStockSymbol(),
                    draft.holding.getQuantity(),
                    draft.holding.getAveragePurchasePrice().setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    draft.currentPrice.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                    draft.investedValue,
                    draft.currentValue,
                    pnl,
                    holdingReturnPct,
                    allocationPct
            );
            holdingResponses.add(response);
        }

        return new PortfolioResponse(
                userId,
                totalInvested.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                totalCurrent.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP),
                totalUnrealizedPnL,
                totalReturnPercentage,
                holdingResponses.size(),
                holdingResponses
        );
    }

    /**
     * Retrieves all holdings for a user formatted as enriched HoldingResponses.
     */
    public List<HoldingResponse> getHoldings(Long userId) {
        return getPortfolio(userId).getHoldings();
    }

    /**
     * Retrieves analytical portfolio performance metrics (best/worst stocks, gainers/losers count).
     */
    public PortfolioPerformanceResponse getPerformance(Long userId) {
        PortfolioResponse portfolio = getPortfolio(userId);
        PortfolioPerformanceResponse performance = new PortfolioPerformanceResponse();

        performance.setUserId(userId);
        performance.setTotalInvestedValue(portfolio.getTotalInvestedValue());
        performance.setTotalCurrentValue(portfolio.getTotalCurrentValue());
        performance.setTotalUnrealizedProfitLoss(portfolio.getTotalUnrealizedProfitLoss());
        performance.setTotalReturnPercentage(portfolio.getTotalReturnPercentage());
        performance.setTotalHoldingsCount(portfolio.getTotalHoldingsCount());

        List<HoldingResponse> holdings = portfolio.getHoldings();
        if (holdings.isEmpty()) {
            performance.setProfitableHoldingsCount(0);
            performance.setLossMakingHoldingsCount(0);
            performance.setBestPerformerSymbol("N/A");
            performance.setBestPerformerReturnPercentage(BigDecimal.ZERO);
            performance.setWorstPerformerSymbol("N/A");
            performance.setWorstPerformerReturnPercentage(BigDecimal.ZERO);
            return performance;
        }

        int profitable = 0;
        int lossMaking = 0;
        HoldingResponse best = holdings.get(0);
        HoldingResponse worst = holdings.get(0);

        for (HoldingResponse h : holdings) {
            if (h.getUnrealizedProfitLoss().compareTo(BigDecimal.ZERO) > 0) {
                profitable++;
            } else if (h.getUnrealizedProfitLoss().compareTo(BigDecimal.ZERO) < 0) {
                lossMaking++;
            }

            if (h.getReturnPercentage().compareTo(best.getReturnPercentage()) > 0) {
                best = h;
            }
            if (h.getReturnPercentage().compareTo(worst.getReturnPercentage()) < 0) {
                worst = h;
            }
        }

        performance.setProfitableHoldingsCount(profitable);
        performance.setLossMakingHoldingsCount(lossMaking);
        performance.setBestPerformerSymbol(best.getStockSymbol());
        performance.setBestPerformerReturnPercentage(best.getReturnPercentage());
        performance.setWorstPerformerSymbol(worst.getStockSymbol());
        performance.setWorstPerformerReturnPercentage(worst.getReturnPercentage());

        return performance;
    }

    /**
     * Builds a Portfolio domain model snapshot from stored holdings and market prices.
     */
    public Portfolio getPortfolioSnapshot(Long userId) {
        PortfolioResponse response = getPortfolio(userId);
        List<Holding> domainHoldings = holdingRepository.findByUserId(userId);
        return new Portfolio(
                userId,
                domainHoldings,
                response.getTotalInvestedValue(),
                response.getTotalCurrentValue(),
                response.getTotalUnrealizedProfitLoss(),
                response.getTotalReturnPercentage()
        );
    }

    /**
     * Safely calculates percentage ratio: (numerator / denominator) * 100.
     * Handles zero denominator gracefully without ArithmeticException.
     */
    private BigDecimal calculatePercentage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
        }
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, DISPLAY_SCALE, RoundingMode.HALF_UP);
    }

    private record HoldingResponseDraft(Holding holding,
                                        BigDecimal currentPrice,
                                        BigDecimal investedValue,
                                        BigDecimal currentValue) {}
}
