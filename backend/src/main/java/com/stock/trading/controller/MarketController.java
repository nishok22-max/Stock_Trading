package com.stock.trading.controller;

import com.stock.trading.model.MarketStatus;
import com.stock.trading.service.MarketService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketController {
    private final MarketService market;

    public MarketController(MarketService market) {
        this.market = market;
    }

    @GetMapping("/status")
    public MarketStatus status() {
        return market.status();
    }
}
