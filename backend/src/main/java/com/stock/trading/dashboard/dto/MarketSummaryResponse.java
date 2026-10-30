package com.stock.trading.dashboard.dto;

import com.stock.trading.port.StockQuote;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Schema(description = "Market summary overview including status and stock quotes")
public record MarketSummaryResponse(
    @Schema(description = "Whether the market is currently open for trading")
    boolean open,

    @Schema(description = "Human-readable market status text (e.g. OPEN, CLOSED)")
    String marketStatus,

    @Schema(description = "Total number of available stocks in the market")
    int totalStocks,

    @Schema(description = "Current server timestamp")
    Instant serverTime,

    @Schema(description = "Expected session close timestamp")
    Instant sessionClose,

    @Schema(description = "Top available stocks in the market with day metrics")
    List<StockSummaryItem> topStocks
) {
    @Schema(description = "Stock quote summary with price change and percentage metrics")
    public record StockSummaryItem(
        @Schema(description = "Stock ticker symbol")
        String symbol,

        @Schema(description = "Full company name")
        String companyName,

        @Schema(description = "Market sector")
        String sector,

        @Schema(description = "Current market price in INR")
        BigDecimal currentPrice,

        @Schema(description = "Previous trading day close price in INR")
        BigDecimal previousClose,

        @Schema(description = "Absolute price change from previous close in INR")
        BigDecimal priceChange,

        @Schema(description = "Percentage price change from previous close")
        BigDecimal percentageChange,

        @Schema(description = "Whether trading is active for this stock")
        boolean active
    ) {
        public static StockSummaryItem from(StockQuote q) {
            BigDecimal change = q.price().subtract(q.previousClose());
            BigDecimal pct = BigDecimal.ZERO;
            if (q.previousClose().compareTo(BigDecimal.ZERO) > 0) {
                pct = change.divide(q.previousClose(), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP);
            }
            return new StockSummaryItem(
                q.symbol(),
                q.companyName(),
                q.sector(),
                q.price().setScale(2, RoundingMode.HALF_UP),
                q.previousClose().setScale(2, RoundingMode.HALF_UP),
                change.setScale(2, RoundingMode.HALF_UP),
                pct,
                q.active()
            );
        }
    }
}

