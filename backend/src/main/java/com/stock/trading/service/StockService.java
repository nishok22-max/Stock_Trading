package com.stock.trading.service;

import com.stock.trading.dto.StockResponse;
import com.stock.trading.exception.TradingException;
import com.stock.trading.model.StockPrice;
import com.stock.trading.repository.MarketRepository;
import com.stock.trading.store.TradingUnitOfWork;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class StockService {
    private final TradingUnitOfWork work;
    private final MarketRepository stocks;

    public StockService(TradingUnitOfWork work, MarketRepository stocks) {
        this.work = work;
        this.stocks = stocks;
    }

    public List<StockResponse> findAll() {
        return work.execute(() -> stocks.findAll().stream().map(StockResponse::from).toList());
    }

    public List<StockResponse> search(String query) {
        if (query == null || query.isBlank())
            throw TradingException.rule("INVALID_SEARCH", "Search query must contain a stock symbol or company name");
        var term = query.trim().toLowerCase(Locale.ROOT);
        return work.execute(() -> stocks.findAll().stream()
                .filter(stock -> stock.symbol().toLowerCase(Locale.ROOT).contains(term)
                        || stock.companyName().toLowerCase(Locale.ROOT).contains(term))
                .map(StockResponse::from).toList());
    }

    public StockResponse find(String symbol) {
        return work.execute(() -> StockResponse.from(stocks.findBySymbol(normalize(symbol))));
    }

    public StockResponse currentPrice(String symbol) {
        return find(symbol);
    }

    public List<StockPrice> history(String symbol) {
        return work.execute(() -> stocks.history(normalize(symbol)));
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank())
            throw TradingException.rule("INVALID_SYMBOL", "Stock symbol cannot be blank");
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
