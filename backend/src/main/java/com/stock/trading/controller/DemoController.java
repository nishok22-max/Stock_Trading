package com.stock.trading.controller;

import com.stock.trading.exception.TradingException;
import com.stock.trading.model.Money;
import com.stock.trading.port.*;
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
    private final MarketGateway market;
    private final TradingService trading;
    public DemoController(TradingUnitOfWork work,MarketGateway market,TradingService trading) { this.work=work; this.market=market; this.trading=trading; }
    @GetMapping("/users")
    public List<DemoUser> users() { return work.execute(()->work.state().accounts.values().stream().map(a->new DemoUser(a.userId(),a.name(),a.kycApproved(),a.blocked())).sorted(Comparator.comparing(DemoUser::name)).toList()); }
    @PutMapping("/stocks/{symbol}/price")
    public StockQuote update(@PathVariable String symbol,@Valid @RequestBody PriceUpdate request) {
        var quote=work.execute(()->{
            var old=market.quote(symbol.toUpperCase(Locale.ROOT));
            if(request.price().subtract(old.previousClose()).abs().compareTo(old.previousClose().multiply(new BigDecimal("0.10")))>0)
                throw TradingException.rule("PRICE_BAND","Price must stay within 10% of the previous close");
            var next=new StockQuote(old.symbol(),old.companyName(),old.sector(),Money.amount(request.price()),old.previousClose(),old.active());
            work.state().stocks.put(next.symbol(),next);
            return next;
        });
        trading.processPending();
        return quote;
    }
}
