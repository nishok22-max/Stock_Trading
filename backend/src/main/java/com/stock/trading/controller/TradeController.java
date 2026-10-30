package com.stock.trading.controller;

import com.stock.trading.model.Trade;
import com.stock.trading.service.OrderService;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeController {
    private final OrderService orders;
    public TradeController(OrderService orders) { this.orders=orders; }
    @GetMapping("/user/{userId}")
    public List<Trade> history(@RequestHeader("X-User-Id") UUID actor,@PathVariable UUID userId) { return orders.trades(actor,userId); }
}
