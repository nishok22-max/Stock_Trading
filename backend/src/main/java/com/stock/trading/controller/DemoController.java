package com.stock.trading.controller;

import com.stock.trading.model.Money;
import com.stock.trading.port.*;
import com.stock.trading.service.MarketDataService;
import com.stock.trading.service.TradingService;
import com.stock.trading.store.TradingUnitOfWork;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

/** Deliberately demo-only. Member 2 must own authenticated quote updates in the integrated app. */
@RestController
@RequestMapping("/api/demo")
@ConditionalOnProperty(name="trading.demo-enabled",havingValue="true",matchIfMissing=true)
public class DemoController {
    public record DemoUser(UUID userId,String name,boolean kycApproved,boolean blocked) {}
    public record PriceUpdate(@NotNull @DecimalMin("0.01") @Digits(integer=9,fraction=2) BigDecimal price) {}
    private final TradingUnitOfWork work;
    private final TradingService trading;
    private final MarketDataService marketData;
    private final java.time.Clock clock;
    public DemoController(TradingUnitOfWork work,TradingService trading,MarketDataService marketData,java.time.Clock clock) {
        this.work=work; this.trading=trading; this.marketData=marketData; this.clock=clock;
    }
    @GetMapping("/users")
    public List<DemoUser> users() { return work.execute(()->work.state().accounts.values().stream().map(a->new DemoUser(a.userId(),a.name(),a.kycApproved(),a.blocked())).sorted(Comparator.comparing(DemoUser::name)).toList()); }
    @PutMapping("/stocks/{symbol}/price")
    public StockQuote update(@PathVariable String symbol,@Valid @RequestBody PriceUpdate request) {
        var quote=marketData.updatePrice(symbol,Money.amount(request.price()),clock.instant());
        trading.processPending();
        return quote;
    }
}
