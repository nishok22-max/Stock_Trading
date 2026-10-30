package com.stocktrading.portfolio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktrading.portfolio.dto.WatchlistRequest;
import com.stocktrading.portfolio.dto.WatchlistResponse;
import com.stocktrading.portfolio.model.WatchlistItem;
import com.stocktrading.portfolio.service.WatchlistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.context.ContextConfiguration;
import com.stocktrading.portfolio.PortfolioApplication;

@WebMvcTest(WatchlistController.class)
@ContextConfiguration(classes = PortfolioApplication.class)
@DisplayName("Phase 9: WatchlistController REST API Tests")
class WatchlistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WatchlistService watchlistService;

    @Test
    @DisplayName("GET /api/watchlist/{userId} - Returns 200 and watchlist list")
    void testGetWatchlist() throws Exception {
        Long userId = 101L;
        WatchlistResponse item = new WatchlistResponse(
                1L, userId, "TCS", new BigDecimal("3900.00"), LocalDateTime.now()
        );

        when(watchlistService.getUserWatchlistWithPrices(userId)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/watchlist/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockSymbol").value("TCS"))
                .andExpect(jsonPath("$[0].currentPrice").value(3900.00));
    }

    @Test
    @DisplayName("POST /api/watchlist - Returns 201 Created on valid addition")
    void testAddToWatchlist() throws Exception {
        WatchlistRequest request = new WatchlistRequest(101L, "INFY");
        WatchlistItem saved = new WatchlistItem(1L, 101L, "INFY");

        when(watchlistService.addToWatchlist(101L, "INFY")).thenReturn(saved);

        mockMvc.perform(post("/api/watchlist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockSymbol").value("INFY"));
    }

    @Test
    @DisplayName("DELETE /api/watchlist/{userId}/{symbol} - Returns 200 OK")
    void testDeleteFromWatchlist() throws Exception {
        when(watchlistService.removeFromWatchlist(101L, "INFY")).thenReturn(true);

        mockMvc.perform(delete("/api/watchlist/101/INFY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Stock 'INFY' removed from watchlist successfully."));
    }
}
