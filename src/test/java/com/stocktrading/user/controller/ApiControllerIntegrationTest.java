package com.stocktrading.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocktrading.user.dto.DepositRequest;
import com.stocktrading.user.dto.LoginRequest;
import com.stocktrading.user.dto.RegisterRequest;
import com.stocktrading.user.dto.WithdrawRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API Controllers Integration Tests")
class ApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/register - Should register user and return 201 Created")
    void testRegisterEndpoint() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Anita Desai",
                "anita" + System.currentTimeMillis() + "@example.com",
                "Secret@123",
                "ABCDE" + (int)(1000 + Math.random() * 9000) + "Z",
                "9876543299",
                "HDFC-776655",
                new BigDecimal("1000.00")
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", notNullValue()))
                .andExpect(jsonPath("$.accountId", notNullValue()))
                .andExpect(jsonPath("$.userName", is("Anita Desai")))
                .andExpect(jsonPath("$.availableBalance", is(1000.00)));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should log in with seeded Rahul user")
    void testLoginEndpoint() throws Exception {
        LoginRequest request = new LoginRequest("rahul@example.com", "Password@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email", is("rahul@example.com")))
                .andExpect(jsonPath("$.account.accountId", is("ACC-5001")));
    }

    @Test
    @DisplayName("GET /api/account/{userId} - Should get account details")
    void testGetAccountEndpoint() throws Exception {
        mockMvc.perform(get("/api/account/USR-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR-1001")))
                .andExpect(jsonPath("$.accountId", is("ACC-5001")))
                .andExpect(jsonPath("$.userName", is("Rahul Sharma")));
    }

    @Test
    @DisplayName("GET /api/account/{userId}/balance - Should return balance details")
    void testGetBalanceEndpoint() throws Exception {
        mockMvc.perform(get("/api/account/USR-1001/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR-1001")))
                .andExpect(jsonPath("$.availableBalance", notNullValue()))
                .andExpect(jsonPath("$.totalBalance", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/account/deposit - Should deposit funds and return updated account")
    void testDepositEndpoint() throws Exception {
        DepositRequest request = new DepositRequest("USR-1001", new BigDecimal("500.00"), "HDFC-892104", "Top-up");

        mockMvc.perform(post("/api/account/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR-1001")));
    }

    @Test
    @DisplayName("POST /api/account/withdraw - Should withdraw funds")
    void testWithdrawEndpoint() throws Exception {
        WithdrawRequest request = new WithdrawRequest("USR-1001", new BigDecimal("200.00"), "HDFC-892104", "Withdraw cash");

        mockMvc.perform(post("/api/account/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR-1001")));
    }

    @Test
    @DisplayName("GET /api/account/{userId}/transactions - Should retrieve transaction history")
    void testGetTransactionsEndpoint() throws Exception {
        mockMvc.perform(get("/api/account/USR-1001/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$", not(empty())));
    }

    @Test
    @DisplayName("POST /api/account/deposit - Should return 400 Bad Request if deposit is below Rs. 100")
    void testDepositValidationFailure() throws Exception {
        DepositRequest request = new DepositRequest("USR-1001", new BigDecimal("50.00"), "HDFC-892104", "Too low");

        mockMvc.perform(post("/api/account/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", notNullValue()));
    }
}
