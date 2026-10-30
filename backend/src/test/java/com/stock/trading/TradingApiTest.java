package com.stock.trading;

import com.stock.trading.port.*;
import com.stock.trading.store.TradingUnitOfWork;
import com.stock.trading.store.TradingUnitOfWork.HoldingKey;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={"trading.demo-enabled=false","trading.scheduler-enabled=false","trading.demo-open-market=false"})
@AutoConfigureMockMvc
@Import(TradingApiTest.TimeConfiguration.class)
class TradingApiTest {
    static final String USER="11111111-1111-4111-8111-111111111111";
    static final String OTHER="22222222-2222-4222-8222-222222222222";
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TradingUnitOfWork work;
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2026-10-30T04:30:00Z"),ZoneOffset.UTC); }
    }
    @BeforeEach void seed() {
        work.execute(()->{
            var s=work.state(); s.accounts.clear();s.holdings.clear();s.orders.clear();s.trades.clear();s.stocks.clear();s.requests.clear();
            var id=UUID.fromString(USER);
            s.accounts.put(id,new AccountSnapshot(id,"API Trader",true,false,new BigDecimal("10000.00"),new BigDecimal("0.00")));
            s.holdings.put(new HoldingKey(id,"TCS"),new HoldingSnapshot(id,"TCS",10,0,new BigDecimal("80.00")));
            s.stocks.put("TCS",new StockQuote("TCS","Tata Consultancy Services","IT",new BigDecimal("100.00"),new BigDecimal("100.00"),true)); return null;
        });
    }
    MockHttpServletRequestBuilder order(String body) { return post("/api/orders").header("X-User-Id",USER).contentType("application/json").content(body); }
    String buy() { return "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":2}"; }
    String limit() { return "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"LIMIT\",\"quantity\":2,\"limitPrice\":90}"; }
    String id(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).get("id").asText(); }

    @Test void allRequiredEndpointsAndAccountUpdatesWorkTogether() throws Exception {
        var result=mvc.perform(order(buy())).andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("EXECUTED")).andExpect(jsonPath("$.executionPrice").value(100)).andReturn();
        var id=id(result);
        mvc.perform(get("/api/orders/"+id).header("X-User-Id",USER)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/orders/user/"+USER).header("X-User-Id",USER)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/trades/user/"+USER).header("X-User-Id",USER)).andExpect(status().isOk()).andExpect(jsonPath("$[0].orderId").value(id)).andExpect(jsonPath("$[0].totalCharges").value(0.12));
        mvc.perform(get("/api/trading/account").header("X-User-Id",USER)).andExpect(status().isOk()).andExpect(jsonPath("$.account.availableBalance").value(9799.88)).andExpect(jsonPath("$.holdings[0].quantity").value(12));
    }
    @Test void pendingOrderCanBeCancelledAndRepeatedCancelConflicts() throws Exception {
        var id=id(mvc.perform(order(limit())).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andReturn());
        mvc.perform(post("/api/orders/"+id+"/cancel").header("X-User-Id",USER)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.reservedCash").value(0));
        mvc.perform(post("/api/orders/"+id+"/cancel").header("X-User-Id",USER)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
    }
    @Test void previewUsesServerChargesWithoutPlacingIntent() throws Exception {
        mvc.perform(post("/api/orders/preview").header("X-User-Id",USER).contentType("application/json").content(limit()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cashToReserve").value(180.11)).andExpect(jsonPath("$.executesImmediately").value(false));
        mvc.perform(get("/api/orders/user/"+USER).header("X-User-Id",USER)).andExpect(jsonPath("$.length()").value(0));
    }
    @ParameterizedTest
    @ValueSource(strings={
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":1.5}",
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":0}",
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"LIMIT\",\"quantity\":1}",
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"LIMIT\",\"quantity\":1,\"limitPrice\":0}",
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"LIMIT\",\"quantity\":1,\"limitPrice\":1.234}",
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":1,\"limitPrice\":100}",
            "{\"symbol\":\"TCS\",\"side\":\"UNKNOWN\",\"type\":\"MARKET\",\"quantity\":1}",
            "{\"symbol\":\"TCS\",\"type\":\"MARKET\",\"quantity\":1}",
            "{\"symbol\":\"TCS\",\"side\":\"BUY\",\"type\":\"MARKET\",\"quantity\":1,\"userId\":\"spoofed\"}",
            "{}", "invalid-json"
    })
    void malformedRequestsReturnStructured400(String body) throws Exception {
        mvc.perform(order(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
    @Test void businessRejectionIsRecordedAndVisible() throws Exception {
        mvc.perform(order(buy().replace("\"quantity\":2","\"quantity\":1000"))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("INSUFFICIENT_FUNDS")));
        mvc.perform(get("/api/orders/user/"+USER).header("X-User-Id",USER)).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void missingIdentityAndInvalidUuidAreBadRequests() throws Exception {
        mvc.perform(post("/api/orders").contentType("application/json").content(buy())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders/not-a-uuid").header("X-User-Id",USER)).andExpect(status().isBadRequest());
    }
    @Test void unknownAndForeignOrdersAreHiddenAndHistoryCannotBeReadAcrossUsers() throws Exception {
        var id=id(mvc.perform(order(limit())).andReturn());
        mvc.perform(get("/api/orders/"+id).header("X-User-Id",OTHER)).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/"+id+"/cancel").header("X-User-Id",OTHER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/"+UUID.randomUUID()).header("X-User-Id",USER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/user/"+USER).header("X-User-Id",OTHER)).andExpect(status().isForbidden());
        mvc.perform(get("/api/trades/user/"+USER).header("X-User-Id",OTHER)).andExpect(status().isForbidden());
    }
    @Test void retryKeyPreservesOneTradeAndPayloadConflictReturns409() throws Exception {
        var first=mvc.perform(order(buy()).header("Idempotency-Key","http-retry")).andExpect(status().isCreated()).andReturn();
        var second=mvc.perform(order(buy()).header("Idempotency-Key","http-retry")).andExpect(status().isCreated()).andReturn();
        assertThat(id(first)).isEqualTo(id(second));
        mvc.perform(order(limit()).header("Idempotency-Key","http-retry")).andExpect(status().isConflict());
    }
    @Test void swaggerIncludesTradingEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/api/orders']").exists()).andExpect(jsonPath("$.paths['/api/trades/user/{userId}']").exists());
    }
    @Test void disabledDemoAndUnknownEndpointsReturn404AndUnsupportedMethods405() throws Exception {
        mvc.perform(get("/api/demo/users")).andExpect(status().isNotFound());
        mvc.perform(get("/api/no-such-endpoint")).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").header("X-User-Id",USER)).andExpect(status().isMethodNotAllowed());
    }
}
