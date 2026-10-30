package com.stock.trading;

import com.stock.trading.dashboard.dto.DashboardResponse;
import com.stock.trading.dashboard.dto.MarketSummaryResponse;
import com.stock.trading.dashboard.dto.RecentOrderResponse;
import com.stock.trading.dashboard.service.DashboardService;
import com.stock.trading.enums.OrderSide;
import com.stock.trading.enums.OrderStatus;
import com.stock.trading.enums.OrderType;
import com.stock.trading.exception.TradingException;
import com.stock.trading.model.Money;
import com.stock.trading.model.Order;
import com.stock.trading.port.AccountGateway;
import com.stock.trading.port.AccountSnapshot;
import com.stock.trading.port.HoldingSnapshot;
import com.stock.trading.port.MarketGateway;
import com.stock.trading.port.StockQuote;
import com.stock.trading.repository.OrderRepository;
import com.stock.trading.store.TradingUnitOfWork;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock private TradingUnitOfWork work;
    @Mock private AccountGateway accounts;
    @Mock private MarketGateway market;
    @Mock private OrderRepository orders;

    private Clock clock;
    private DashboardService dashboardService;

    private final UUID userId = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private final Instant now = Instant.parse("2026-10-30T10:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(now, ZoneOffset.UTC);
        // By default, make unit of work execute the supplier directly
        when(work.execute(any())).thenAnswer(invocation -> {
            java.util.function.Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });
        dashboardService = new DashboardService(work, accounts, market, orders, clock);
    }

    @Test
    @DisplayName("Should aggregate dashboard summary accurately with holdings and orders")
    void shouldAggregateDashboardSummaryAccurately() {
        // Arrange
        AccountSnapshot account = new AccountSnapshot(
                userId, "Aarav Mehta", true, false,
                new BigDecimal("50000.00"), new BigDecimal("1000.00")
        );
        when(accounts.account(userId)).thenReturn(account);

        List<HoldingSnapshot> holdings = List.of(
                new HoldingSnapshot(userId, "TCS", 10, 0, new BigDecimal("3000.00")),
                new HoldingSnapshot(userId, "INFY", 20, 0, new BigDecimal("1500.00"))
        );
        when(accounts.holdings(userId)).thenReturn(holdings);

        when(market.quote("TCS")).thenReturn(new StockQuote("TCS", "Tata Consultancy Services", "IT",
                new BigDecimal("3500.00"), new BigDecimal("3400.00"), true));
        when(market.quote("INFY")).thenReturn(new StockQuote("INFY", "Infosys Limited", "IT",
                new BigDecimal("1600.00"), new BigDecimal("1550.00"), true));
        when(market.isOpen(now)).thenReturn(true);

        Order sampleOrder = new Order(
                UUID.randomUUID(), userId, "TCS", OrderSide.BUY, OrderType.MARKET,
                10, null, OrderStatus.EXECUTED, Money.ZERO, 0,
                new BigDecimal("3500.00"), Money.ZERO, Money.ZERO, UUID.randomUUID(),
                null, now.minusSeconds(3600), now.minusSeconds(3500), now.plusSeconds(3600)
        );
        when(orders.byUser(userId)).thenReturn(List.of(sampleOrder));

        // Act
        DashboardResponse response = dashboardService.getDashboardSummary(userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.name()).isEqualTo("Aarav Mehta");
        assertThat(response.kycApproved()).isTrue();
        assertThat(response.blocked()).isFalse();
        assertThat(response.availableBalance()).isEqualByComparingTo("50000.00");
        assertThat(response.blockedBalance()).isEqualByComparingTo("1000.00");

        // Total Invested = (10 * 3000) + (20 * 1500) = 30000 + 30000 = 60000.00
        assertThat(response.totalInvestedValue()).isEqualByComparingTo("60000.00");

        // Current Value = (10 * 3500) + (20 * 1600) = 35000 + 32000 = 67000.00
        assertThat(response.currentPortfolioValue()).isEqualByComparingTo("67000.00");

        // Profit = 67000 - 60000 = 7000.00
        assertThat(response.totalProfitLoss()).isEqualByComparingTo("7000.00");

        // P/L % = (7000 / 60000) * 100 = 11.67%
        assertThat(response.totalProfitLossPercentage()).isEqualByComparingTo("11.67");

        assertThat(response.marketOpen()).isTrue();
        assertThat(response.totalHoldings()).isEqualTo(2);
        assertThat(response.totalOrders()).isEqualTo(1);
        assertThat(response.pendingOrders()).isEqualTo(0);
        assertThat(response.recentOrders()).hasSize(1);
        assertThat(response.recentOrders().get(0).stockSymbol()).isEqualTo("TCS");
    }

    @Test
    @DisplayName("Should handle empty holdings and empty orders gracefully")
    void shouldHandleEmptyHoldingsAndOrders() {
        AccountSnapshot account = new AccountSnapshot(
                userId, "New Trader", true, false,
                new BigDecimal("10000.00"), BigDecimal.ZERO
        );
        when(accounts.account(userId)).thenReturn(account);
        when(accounts.holdings(userId)).thenReturn(List.of());
        when(orders.byUser(userId)).thenReturn(List.of());
        when(market.isOpen(now)).thenReturn(false);

        DashboardResponse response = dashboardService.getDashboardSummary(userId);

        assertThat(response).isNotNull();
        assertThat(response.totalInvestedValue()).isEqualByComparingTo("0.00");
        assertThat(response.currentPortfolioValue()).isEqualByComparingTo("0.00");
        assertThat(response.totalProfitLoss()).isEqualByComparingTo("0.00");
        assertThat(response.totalProfitLossPercentage()).isEqualByComparingTo("0.00");
        assertThat(response.totalHoldings()).isEqualTo(0);
        assertThat(response.totalOrders()).isEqualTo(0);
        assertThat(response.pendingOrders()).isEqualTo(0);
        assertThat(response.recentOrders()).isEmpty();
        assertThat(response.marketOpen()).isFalse();
    }

    @Test
    @DisplayName("Should throw exception when user account is not found")
    void shouldThrowExceptionWhenUserNotFound() {
        when(accounts.account(userId)).thenThrow(TradingException.notFound("Trading account not found"));

        assertThatThrownBy(() -> dashboardService.getDashboardSummary(userId))
                .isInstanceOf(TradingException.class)
                .hasMessageContaining("Trading account not found");
    }

    @Test
    @DisplayName("Should retrieve recent orders with given limit")
    void shouldRetrieveRecentOrdersWithLimit() {
        Order order1 = new Order(
                UUID.randomUUID(), userId, "TCS", OrderSide.BUY, OrderType.MARKET,
                5, null, OrderStatus.EXECUTED, Money.ZERO, 0,
                new BigDecimal("3500.00"), Money.ZERO, Money.ZERO, UUID.randomUUID(),
                null, now.minusSeconds(100), now.minusSeconds(90), now.plusSeconds(3600)
        );
        Order order2 = new Order(
                UUID.randomUUID(), userId, "INFY", OrderSide.BUY, OrderType.LIMIT,
                10, new BigDecimal("1500.00"), OrderStatus.PENDING, new BigDecimal("15000.00"), 0,
                null, Money.ZERO, Money.ZERO, null,
                null, now.minusSeconds(50), now.minusSeconds(50), now.plusSeconds(3600)
        );
        when(orders.byUser(userId)).thenReturn(List.of(order1, order2));

        List<RecentOrderResponse> recent = dashboardService.getRecentOrders(userId, 1);

        assertThat(recent).hasSize(1);
        assertThat(recent.get(0).stockSymbol()).isEqualTo("INFY"); // Most recent first
    }

    @Test
    @DisplayName("Should retrieve market summary with price change calculations")
    void shouldRetrieveMarketSummary() {
        when(market.isOpen(now)).thenReturn(true);
        when(market.sessionClose(now)).thenReturn(now.plusSeconds(18000));
        when(market.stocks()).thenReturn(List.of(
                new StockQuote("TCS", "Tata Consultancy Services", "IT",
                        new BigDecimal("3500.00"), new BigDecimal("3400.00"), true),
                new StockQuote("RELIANCE", "Reliance Industries", "Energy",
                        new BigDecimal("2900.00"), new BigDecimal("3000.00"), true)
        ));

        MarketSummaryResponse summary = dashboardService.getMarketSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.open()).isTrue();
        assertThat(summary.marketStatus()).isEqualTo("OPEN");
        assertThat(summary.totalStocks()).isEqualTo(2);
        assertThat(summary.topStocks()).hasSize(2);

        // TCS: price 3500, prevClose 3400 => change +100.00, pct +2.94%
        var tcs = summary.topStocks().stream().filter(s -> s.symbol().equals("TCS")).findFirst().orElseThrow();
        assertThat(tcs.priceChange()).isEqualByComparingTo("100.00");
        assertThat(tcs.percentageChange()).isEqualByComparingTo("2.94");

        // RELIANCE: price 2900, prevClose 3000 => change -100.00, pct -3.33%
        var rel = summary.topStocks().stream().filter(s -> s.symbol().equals("RELIANCE")).findFirst().orElseThrow();
        assertThat(rel.priceChange()).isEqualByComparingTo("-100.00");
        assertThat(rel.percentageChange()).isEqualByComparingTo("-3.33");
    }
}

