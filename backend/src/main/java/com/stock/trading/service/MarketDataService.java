package com.stock.trading.service;

import com.stock.trading.exception.TradingException;
import com.stock.trading.model.StockPrice;
import com.stock.trading.port.StockQuote;
import com.stock.trading.repository.MarketRepository;
import com.stock.trading.store.TradingUnitOfWork;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class MarketDataService {
    private static final Set<String> SECTORS = Set.of("IT", "BANKING", "PHARMA", "ENERGY", "AUTO");
    private final TradingUnitOfWork work;
    private final MarketRepository stocks;

    public MarketDataService(TradingUnitOfWork work, MarketRepository stocks) {
        this.work = work;
        this.stocks = stocks;
    }

    public StockQuote createStock(String symbol, String companyName, String sector,
                                  BigDecimal price, BigDecimal previousClose, boolean active) {
        var normalizedSymbol = required(symbol, "Stock symbol").toUpperCase(Locale.ROOT);
        if (!normalizedSymbol.matches("[A-Z][A-Z0-9.-]{0,9}"))
            throw TradingException.rule("INVALID_SYMBOL", "Stock symbol must be 1 to 10 letters, digits, dots, or hyphens");
        var normalizedName = required(companyName, "Company name");
        var normalizedSector = required(sector, "Sector").toUpperCase(Locale.ROOT);
        if (!SECTORS.contains(normalizedSector))
            throw TradingException.rule("INVALID_SECTOR", "Sector must be IT, BANKING, PHARMA, ENERGY, or AUTO");
        requirePrice(price, "Stock price");
        requirePrice(previousClose, "Previous closing price");
        var quote = new StockQuote(normalizedSymbol, normalizedName, normalizedSector, price, previousClose, active);
        return work.execute(() -> {
            stocks.add(quote);
            return quote;
        });
    }

    public StockQuote updatePrice(String symbol, BigDecimal price, Instant timestamp) {
        requirePrice(price, "Stock price");
        return work.execute(() -> {
            var old = stocks.findBySymbol(normalize(symbol));
            var maximumChange = old.previousClose().multiply(new BigDecimal("0.10"));
            if (price.subtract(old.previousClose()).abs().compareTo(maximumChange) > 0)
                throw TradingException.rule("PRICE_BAND", "Price must stay within 10% of the previous close");
            var next = new StockQuote(old.symbol(), old.companyName(), old.sector(), price, old.previousClose(), old.active());
            stocks.save(next);
            stocks.recordPrice(new StockPrice(old.symbol(), price, old.price(), timestamp));
            return next;
        });
    }

    public StockQuote updateStatus(String symbol, boolean active) {
        return work.execute(() -> {
            var old = stocks.findBySymbol(normalize(symbol));
            var next = new StockQuote(old.symbol(), old.companyName(), old.sector(), old.price(), old.previousClose(), active);
            stocks.save(next);
            return next;
        });
    }

    private void requirePrice(BigDecimal price, String label) {
        if (price == null || price.signum() <= 0 || price.stripTrailingZeros().scale() > 2)
            throw TradingException.rule("INVALID_PRICE", label + " must be positive with at most two decimal places");
    }

    private String required(String value, String label) {
        if (value == null || value.isBlank()) throw TradingException.rule("INVALID_REQUEST", label + " cannot be blank");
        return value.trim();
    }

    private String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) throw TradingException.rule("INVALID_SYMBOL", "Stock symbol cannot be blank");
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
