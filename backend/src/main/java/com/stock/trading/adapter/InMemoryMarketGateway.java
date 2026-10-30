package com.stock.trading.adapter;

import com.stock.trading.port.*;
import com.stock.trading.exception.TradingException;
import com.stock.trading.store.TradingUnitOfWork;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryMarketGateway implements MarketGateway {
    public static final ZoneId ZONE=ZoneId.of("Asia/Kolkata");
    private final TradingUnitOfWork work;
    private final boolean demoOpen;
    public InMemoryMarketGateway(TradingUnitOfWork work, @Value("${trading.demo-open-market:false}") boolean demoOpen) {
        this.work=work; this.demoOpen=demoOpen;
    }
    public StockQuote quote(String symbol) {
        var stock=work.state().stocks.get(symbol);
        if(stock==null) throw TradingException.notFound("Stock symbol not found");
        return stock;
    }
    public List<StockQuote> stocks() { return work.state().stocks.values().stream().sorted(Comparator.comparing(StockQuote::symbol)).toList(); }
    public boolean isOpen(Instant now) {
        if(demoOpen) return true;
        var date=now.atZone(ZONE);
        return date.getDayOfWeek()!=DayOfWeek.SATURDAY && date.getDayOfWeek()!=DayOfWeek.SUNDAY &&
                !date.toLocalTime().isBefore(LocalTime.of(9,15)) && date.toLocalTime().isBefore(LocalTime.of(15,30));
    }
    public Instant sessionClose(Instant now) {
        // Explicit demonstration mode uses a rolling session so it works after hours.
        return demoOpen ? now.plus(Duration.ofHours(6)) : now.atZone(ZONE).toLocalDate().atTime(15,30).atZone(ZONE).toInstant();
    }
}
