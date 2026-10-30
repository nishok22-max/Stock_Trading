package com.stock.trading.dashboard.service;

import com.stock.trading.dashboard.dto.DashboardResponse;
import com.stock.trading.dashboard.dto.MarketSummaryResponse;
import com.stock.trading.dashboard.dto.RecentOrderResponse;
import com.stock.trading.dto.OrderResponse;
import com.stock.trading.enums.OrderStatus;
import com.stock.trading.model.Order;
import com.stock.trading.port.AccountGateway;
import com.stock.trading.port.AccountSnapshot;
import com.stock.trading.port.HoldingSnapshot;
import com.stock.trading.port.MarketGateway;
import com.stock.trading.port.StockQuote;
import com.stock.trading.repository.OrderRepository;
import com.stock.trading.store.TradingUnitOfWork;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final TradingUnitOfWork work;
    private final AccountGateway accounts;
    private final MarketGateway market;
    private final OrderRepository orders;
    private final Clock clock;

    public DashboardService(
            TradingUnitOfWork work,
            AccountGateway accounts,
            MarketGateway market,
            OrderRepository orders,
            Clock clock) {
        this.work = work;
        this.accounts = accounts;
        this.market = market;
        this.orders = orders;
        this.clock = clock;
    }

    /**
     * Aggregates dashboard summary for a given user.
     * Computes balances, invested capital, current portfolio value,
     * total P/L, market state, and recent orders.
     */
    public DashboardResponse getDashboardSummary(UUID userId) {
        return work.execute(() -> {
            AccountSnapshot account = accounts.account(userId);
            List<HoldingSnapshot> holdings = accounts.holdings(userId);
            Instant now = clock.instant();
            boolean open = market.isOpen(now);

            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal currentPortfolioValue = BigDecimal.ZERO;

            for (HoldingSnapshot h : holdings) {
                if (h.quantity() <= 0) continue;
                BigDecimal invested = h.averageBuyPrice().multiply(BigDecimal.valueOf(h.quantity()));
                totalInvested = totalInvested.add(invested);

                BigDecimal currentPrice = h.averageBuyPrice();
                try {
                    StockQuote quote = market.quote(h.symbol());
                    if (quote != null && quote.price() != null) {
                        currentPrice = quote.price();
                    }
                } catch (Exception ignored) {
                    // Fall back to average purchase price if quote is unreachable
                }
                BigDecimal holdingVal = currentPrice.multiply(BigDecimal.valueOf(h.quantity()));
                currentPortfolioValue = currentPortfolioValue.add(holdingVal);
            }

            BigDecimal totalProfitLoss = currentPortfolioValue.subtract(totalInvested);
            BigDecimal totalProfitLossPercentage = BigDecimal.ZERO;
            if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
                totalProfitLossPercentage = totalProfitLoss
                        .divide(totalInvested, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            List<Order> userOrders = orders.byUser(userId);
            int totalOrderCount = userOrders.size();
            int pendingCount = (int) userOrders.stream()
                    .filter(o -> o.status() == OrderStatus.PENDING)
                    .count();

            List<RecentOrderResponse> recentOrders = userOrders.stream()
                    .sorted(Comparator.comparing(Order::createdAt).reversed())
                    .limit(10)
                    .map(OrderResponse::from)
                    .map(RecentOrderResponse::from)
                    .toList();

            return new DashboardResponse(
                account.userId(),
                account.name(),
                account.kycApproved(),
                account.blocked(),
                account.availableBalance().setScale(2, RoundingMode.HALF_UP),
                account.blockedBalance().setScale(2, RoundingMode.HALF_UP),
                totalInvested.setScale(2, RoundingMode.HALF_UP),
                currentPortfolioValue.setScale(2, RoundingMode.HALF_UP),
                totalProfitLoss.setScale(2, RoundingMode.HALF_UP),
                totalProfitLossPercentage,
                open,
                holdings.size(),
                totalOrderCount,
                pendingCount,
                recentOrders,
                now
            );
        });
    }

    /**
     * Retrieves the recent orders for a user.
     */
    public List<RecentOrderResponse> getRecentOrders(UUID userId, int limit) {
        return work.execute(() -> {
            accounts.account(userId); // Validates user existence
            return orders.byUser(userId).stream()
                    .sorted(Comparator.comparing(Order::createdAt).reversed())
                    .limit(limit > 0 ? limit : 10)
                    .map(OrderResponse::from)
                    .map(RecentOrderResponse::from)
                    .toList();
        });
    }

    /**
     * Retrieves the market summary overview.
     */
    public MarketSummaryResponse getMarketSummary() {
        return work.execute(() -> {
            Instant now = clock.instant();
            boolean open = market.isOpen(now);
            List<StockQuote> quotes = market.stocks();
            List<MarketSummaryResponse.StockSummaryItem> topStocks = quotes.stream()
                    .map(MarketSummaryResponse.StockSummaryItem::from)
                    .toList();

            return new MarketSummaryResponse(
                open,
                open ? "OPEN" : "CLOSED",
                quotes.size(),
                now,
                market.sessionClose(now),
                topStocks
            );
        });
    }
}

