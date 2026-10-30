package com.stock.trading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stock.trading.port.*;
import com.stock.trading.store.TradingUnitOfWork;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "trading.demo-enabled=false",
        "trading.scheduler-enabled=false",
        "trading.demo-open-market=false"
})
@AutoConfigureMockMvc
@Import(DashboardIntegrationTest.TimeConfiguration.class)
class DashboardIntegrationTest {

    static final String USER = "11111111-1111-4111-8111-111111111111";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private TradingUnitOfWork work;

    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        Clock testClock() {
            // Market hours: 10:00 AM IST (04:30 UTC)
            return Clock.fixed(Instant.parse("2026-10-30T04:30:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void seed() {
        work.execute(() -> {
            var s = work.state();
            s.accounts.clear();
            s.holdings.clear();
            s.orders.clear();
            s.trades.clear();
            s.stocks.clear();
            s.requests.clear();

            var id = UUID.fromString(USER);
            // Starting balance ₹100,000, 0 holdings
            s.accounts.put(id, new AccountSnapshot(id, "Integration Trader", true, false,
                    new BigDecimal("100000.00"), BigDecimal.ZERO));
            // Pre-seed TCS at ₹3,000
            s.stocks.put("TCS", new StockQuote("TCS", "Tata Consultancy Services", "IT",
                    new BigDecimal("3000.00"), new BigDecimal("2950.00"), true));
            return null;
        });
    }

    @Test
    @DisplayName("Complete end-to-end flow: Deposit -> View stocks -> Buy -> Dashboard updates -> Sell -> Dashboard updates")
    void shouldExecuteFullTradingAndDashboardLifecycle() throws Exception {
        // Step 1: Initial dashboard check: 0 holdings, 0 invested, balance 100,000
        mvc.perform(get("/api/dashboard/" + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableBalance").value(100000.00))
                .andExpect(jsonPath("$.totalInvestedValue").value(0.00))
                .andExpect(jsonPath("$.currentPortfolioValue").value(0.00))
                .andExpect(jsonPath("$.totalHoldings").value(0))
                .andExpect(jsonPath("$.totalOrders").value(0));

        // Step 2: Check market summary
        mvc.perform(get("/api/dashboard/market-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(true))
                .andExpect(jsonPath("$.topStocks[0].symbol").value("TCS"))
                .andExpect(jsonPath("$.topStocks[0].currentPrice").value(3000.00));

        // Step 3: Place a BUY order for 10 shares of TCS at market price (₹3,000 * 10 = ₹30,000)
        String buyPayload = """
                {
                    "symbol": "TCS",
                    "side": "BUY",
                    "type": "MARKET",
                    "quantity": 10
                }
                """;
        var buyResult = mvc.perform(post("/api/orders")
                .header("X-User-Id", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(buyPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andReturn();

        // Step 4: Verify dashboard reflects the purchased shares and reduced cash
        var dashAfterBuy = mvc.perform(get("/api/dashboard/" + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHoldings").value(1))
                .andExpect(jsonPath("$.totalOrders").value(1))
                .andExpect(jsonPath("$.totalInvestedValue").value(30000.00))
                .andExpect(jsonPath("$.currentPortfolioValue").value(30000.00))
                .andExpect(jsonPath("$.recentOrders[0].stockSymbol").value("TCS"))
                .andExpect(jsonPath("$.recentOrders[0].side").value("BUY"))
                .andReturn();

        // Available balance is 100,000 - 30,000 - brokerage/taxes
        var treeAfterBuy = json.readTree(dashAfterBuy.getResponse().getContentAsString());
        double balanceAfterBuy = treeAfterBuy.get("availableBalance").asDouble();
        assertThat(balanceAfterBuy).isLessThan(70000.00); // 100000 - 30000 - charges

        // Step 5: Place a SELL order for 5 shares of TCS at market price
        String sellPayload = """
                {
                    "symbol": "TCS",
                    "side": "SELL",
                    "type": "MARKET",
                    "quantity": 5
                }
                """;
        mvc.perform(post("/api/orders")
                .header("X-User-Id", USER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sellPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUTED"));

        // Step 6: Verify dashboard reflects updated position (5 shares remaining)
        mvc.perform(get("/api/dashboard/" + USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHoldings").value(1))
                .andExpect(jsonPath("$.totalOrders").value(2))
                .andExpect(jsonPath("$.totalInvestedValue").value(15000.00)) // 5 shares * 3000
                .andExpect(jsonPath("$.currentPortfolioValue").value(15000.00))
                .andExpect(jsonPath("$.recentOrders[0].stockSymbol").value("TCS"))
                .andExpect(jsonPath("$.recentOrders[0].side").value("SELL"));
    }
}

