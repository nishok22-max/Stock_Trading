package com.stocktrading.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktrading.portfolio.dto.HoldingResponse;
import com.stocktrading.portfolio.dto.PortfolioResponse;
import com.stocktrading.portfolio.integration.ExecutedTrade;
import com.stocktrading.portfolio.integration.TradeSide;
import com.stocktrading.portfolio.model.Holding;
import com.stocktrading.portfolio.service.HoldingService;
import com.stocktrading.portfolio.service.PortfolioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ContextConfiguration;
import com.stocktrading.portfolio.PortfolioApplication;

@WebMvcTest(PortfolioController.class)
@ContextConfiguration(classes = PortfolioApplication.class)
@DisplayName("Phase 9: PortfolioController REST API Tests")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PortfolioService portfolioService;

    @MockBean
    private HoldingService holdingService;

    @Test
    @DisplayName("GET /api/portfolio/{userId} - Returns 200 and PortfolioResponse JSON")
    void testGetPortfolio() throws Exception {
        Long userId = 101L;
        PortfolioResponse mockResponse = new PortfolioResponse(
                userId,
                new BigDecimal("50000.00"),
                new BigDecimal("55000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("10.00"),
                1,
                List.of(new HoldingResponse("TCS", 10, new BigDecimal("3500.00"), new BigDecimal("3800.00"),
                        new BigDecimal("35000.00"), new BigDecimal("38000.00"), new BigDecimal("3000.00"),
                        new BigDecimal("8.57"), new BigDecimal("100.00")))
        );

        when(portfolioService.getPortfolio(userId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/portfolio/" + userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(101))
                .andExpect(jsonPath("$.totalInvestedValue").value(50000.00))
                .andExpect(jsonPath("$.totalCurrentValue").value(55000.00))
                .andExpect(jsonPath("$.totalUnrealizedProfitLoss").value(5000.00))
                .andExpect(jsonPath("$.totalReturnPercentage").value(10.00));
    }

    @Test
    @DisplayName("POST /api/portfolio/trades/execute - Returns 200 on valid executed trade")
    void testExecuteTrade() throws Exception {
        ExecutedTrade trade = new ExecutedTrade(
                1L, 101L, "TCS", TradeSide.BUY, 5, new BigDecimal("3600.00"), null
        );
        Holding updated = new Holding(101L, "TCS", 5, new BigDecimal("3600.00"));

        when(holdingService.processTrade(any(ExecutedTrade.class))).thenReturn(updated);

        mockMvc.perform(post("/api/portfolio/trades/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trade)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trade processed successfully and holding updated."));
    }
}
