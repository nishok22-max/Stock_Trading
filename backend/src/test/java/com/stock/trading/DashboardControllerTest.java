package com.stock.trading;

import com.stock.trading.port.*;
import com.stock.trading.store.TradingUnitOfWork;
import com.stock.trading.store.TradingUnitOfWork.HoldingKey;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "trading.demo-enabled=false",
        "trading.scheduler-enabled=false",
        "trading.demo-open-market=false"
})
@AutoConfigureMockMvc
@Import(DashboardControllerTest.TimeConfiguration.class)
class DashboardControllerTest {

    static final String USER = "11111111-1111-4111-8111-111111111111";
    static final String NON_EXISTENT = "99999999-9999-9999-9999-999999999999";

    @Autowired private MockMvc mvc;
    @Autowired private TradingUnitOfWork work;

    @TestConfiguration
    static class TimeConfiguration {
        @Bean
        @Primary
        Clock testClock() {
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
            s.accounts.put(id, new AccountSnapshot(id, "Dashboard Tester", true, false,
                    new BigDecimal("75000.00"), new BigDecimal("5000.00")));
            s.holdings.put(new HoldingKey(id, "TCS"), new HoldingSnapshot(id, "TCS", 10, 0, new BigDecimal("3200.00")));
            s.stocks.put("TCS", new StockQuote("TCS", "Tata Consultancy Services", "IT",
                    new BigDecimal("3500.00"), new BigDecimal("3400.00"), true));
            return null;
        });
    }

    @Test
    @DisplayName("GET /api/dashboard/{userId} should return complete dashboard summary")
    void shouldReturnDashboardSummary() throws Exception {
        mvc.perform(get("/api/dashboard/" + USER)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER))
                .andExpect(jsonPath("$.name").value("Dashboard Tester"))
                .andExpect(jsonPath("$.kycApproved").value(true))
                .andExpect(jsonPath("$.blocked").value(false))
                .andExpect(jsonPath("$.availableBalance").value(75000.00))
                .andExpect(jsonPath("$.blockedBalance").value(5000.00))
                .andExpect(jsonPath("$.totalInvestedValue").value(32000.00))
                .andExpect(jsonPath("$.currentPortfolioValue").value(35000.00))
                .andExpect(jsonPath("$.totalProfitLoss").value(3000.00))
                .andExpect(jsonPath("$.totalProfitLossPercentage").value(9.38))
                .andExpect(jsonPath("$.totalHoldings").value(1));
    }

    @Test
    @DisplayName("GET /api/dashboard/{userId} with non-existent user returns 404")
    void shouldReturn404ForNonExistentUser() throws Exception {
        mvc.perform(get("/api/dashboard/" + NON_EXISTENT)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/dashboard/{userId}/recent-orders should return recent orders")
    void shouldReturnRecentOrders() throws Exception {
        mvc.perform(get("/api/dashboard/" + USER + "/recent-orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/dashboard/market-summary should return market summary")
    void shouldReturnMarketSummary() throws Exception {
        mvc.perform(get("/api/dashboard/market-summary")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalStocks").value(1))
                .andExpect(jsonPath("$.topStocks[0].symbol").value("TCS"))
                .andExpect(jsonPath("$.topStocks[0].priceChange").value(100.00));
    }
}

