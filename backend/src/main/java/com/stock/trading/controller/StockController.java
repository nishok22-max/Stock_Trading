package com.stock.trading.controller;

import com.stock.trading.dto.StockResponse;
import com.stock.trading.model.StockPrice;
import com.stock.trading.service.StockService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockService stocks;

    public StockController(StockService stocks) {
        this.stocks = stocks;
    }

    @GetMapping
    public List<StockResponse> all() {
        return stocks.findAll();
    }

    @GetMapping("/search")
    public List<StockResponse> search(@RequestParam("q") String query) {
        return stocks.search(query);
    }

    @GetMapping("/{symbol}")
    public StockResponse details(@PathVariable String symbol) {
        return stocks.find(symbol);
    }

    @GetMapping("/{symbol}/price")
    public StockResponse price(@PathVariable String symbol) {
        return stocks.currentPrice(symbol);
    }

    @GetMapping("/{symbol}/history")
    public List<StockPrice> history(@PathVariable String symbol) {
        return stocks.history(symbol);
    }
}
