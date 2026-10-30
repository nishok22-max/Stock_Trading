package com.stock.trading.controller;

import com.stock.trading.port.*;
import com.stock.trading.store.TradingUnitOfWork;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trading")
public class TradingViewController {
    public record AccountView(AccountSnapshot account,List<HoldingSnapshot> holdings) {}
    public record MarketView(boolean open,boolean demoOpenMarket,Instant serverTime,Instant sessionClose,List<StockQuote> stocks) {}
    private final TradingUnitOfWork work;
    private final AccountGateway accounts;
    private final MarketGateway market;
    private final Clock clock;
    private final boolean demoOpen;
    public TradingViewController(TradingUnitOfWork work,AccountGateway accounts,MarketGateway market,Clock clock,@Value("${trading.demo-open-market:false}") boolean demoOpen) {
        this.work=work; this.accounts=accounts; this.market=market; this.clock=clock; this.demoOpen=demoOpen;
    }
    @GetMapping("/account")
    public AccountView account(@RequestHeader("X-User-Id") UUID userId) { return work.execute(()->new AccountView(accounts.account(userId),accounts.holdings(userId))); }
    @GetMapping("/market")
    public MarketView market() { return work.execute(()->{ var now=clock.instant(); return new MarketView(market.isOpen(now),demoOpen,now,market.sessionClose(now),market.stocks()); }); }
}
