package com.stock.trading.repository;

import com.stock.trading.exception.TradingException;
import com.stock.trading.model.StockPrice;
import com.stock.trading.port.StockQuote;
import com.stock.trading.store.TradingUnitOfWork;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class MarketRepository {
    private final TradingUnitOfWork work;

    public MarketRepository(TradingUnitOfWork work) {
        this.work = work;
    }

    public StockQuote findBySymbol(String symbol) {
        var quote = work.state().stocks.get(symbol);
        if (quote == null) throw TradingException.notFound("Stock symbol not found: " + symbol);
        return quote;
    }

    public List<StockQuote> findAll() {
        return work.state().stocks.values().stream().sorted(Comparator.comparing(StockQuote::symbol)).toList();
    }

    public void add(StockQuote quote) {
        if (work.state().stocks.containsKey(quote.symbol()))
            throw TradingException.conflict("Stock symbol already exists: " + quote.symbol());
        work.state().stocks.put(quote.symbol(), quote);
    }

    public void save(StockQuote quote) {
        work.state().stocks.put(quote.symbol(), quote);
    }

    public void recordPrice(StockPrice price) {
        work.state().stockPrices.computeIfAbsent(price.symbol(), ignored -> new ArrayList<>()).add(price);
    }

    public List<StockPrice> history(String symbol) {
        findBySymbol(symbol);
        return work.state().stockPrices.getOrDefault(symbol, List.of()).stream()
                .sorted(Comparator.comparing(StockPrice::timestamp)).toList();
    }
}
