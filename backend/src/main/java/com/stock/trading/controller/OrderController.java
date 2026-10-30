package com.stock.trading.controller;

import com.stock.trading.dto.*;
import com.stock.trading.service.*;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final TradingService trading;
    private final OrderService orders;
    public OrderController(TradingService trading,OrderService orders) { this.trading=trading; this.orders=orders; }
    @PostMapping
    @Operation(summary="Place an order",description="Creates an EXECUTED, PENDING or REJECTED order. X-User-Id is a trusted demo identity, to be replaced by Member 1 authentication. Idempotency-Key is optional and scoped to the user.")
    public ResponseEntity<OrderResponse> place(@RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody PlaceOrderRequest request) {
        var response=trading.place(userId,request,key);
        return ResponseEntity.created(URI.create("/api/orders/"+response.id())).body(response);
    }
    @PostMapping("/preview")
    @Operation(summary="Validate and price an order without reserving funds or shares")
    public OrderPreview preview(@RequestHeader("X-User-Id") UUID userId,@Valid @RequestBody PlaceOrderRequest request) { return trading.preview(userId,request); }
    @GetMapping("/{orderId}")
    public OrderResponse get(@RequestHeader("X-User-Id") UUID userId,@PathVariable UUID orderId) { return orders.get(userId,orderId); }
    @GetMapping("/user/{userId}")
    public List<OrderResponse> history(@RequestHeader("X-User-Id") UUID actor,@PathVariable UUID userId) { return orders.history(actor,userId); }
    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(@RequestHeader("X-User-Id") UUID actor,@PathVariable UUID orderId) { return orders.cancel(actor,orderId); }
}
