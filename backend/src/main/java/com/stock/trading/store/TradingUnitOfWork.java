package com.stock.trading.store;

import com.stock.trading.model.*;
import com.stock.trading.port.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** Single-process serializable transactions. Immutable values make shallow map copies sufficient. */
@Component
public class TradingUnitOfWork {
    public record HoldingKey(UUID userId, String symbol) {}
    public record RequestKey(UUID userId, String key) {}
    public record IdempotencyEntry(String fingerprint, UUID orderId) {}
    public static final class State {
        public final Map<UUID, AccountSnapshot> accounts;
        public final Map<HoldingKey, HoldingSnapshot> holdings;
        public final Map<String, StockQuote> stocks;
        public final Map<String, List<StockPrice>> stockPrices;
        public final Map<UUID, Order> orders;
        public final Map<UUID, Trade> trades;
        public final Map<RequestKey, IdempotencyEntry> requests;
        private State() {
            accounts=new HashMap<>(); holdings=new HashMap<>(); stocks=new HashMap<>(); stockPrices=new HashMap<>();
            orders=new HashMap<>(); trades=new HashMap<>(); requests=new HashMap<>();
        }
        private State(State old) {
            accounts=new HashMap<>(old.accounts); holdings=new HashMap<>(old.holdings); stocks=new HashMap<>(old.stocks);
            stockPrices=new HashMap<>(); old.stockPrices.forEach((symbol, prices)->stockPrices.put(symbol,new ArrayList<>(prices)));
            orders=new HashMap<>(old.orders); trades=new HashMap<>(old.trades); requests=new HashMap<>(old.requests);
        }
    }
    private final ReentrantLock lock = new ReentrantLock(true);
    private final ThreadLocal<State> transaction = new ThreadLocal<>();
    private State committed = new State();

    public <T> T execute(Supplier<T> operation) {
        if (transaction.get() != null) throw new IllegalStateException("Nested transactions are not supported");
        lock.lock();
        try {
            transaction.set(new State(committed));
            T result = operation.get();
            // Copy again so a collaborator retaining a staged map cannot mutate committed state later.
            committed = new State(transaction.get());
            return result;
        } finally { transaction.remove(); lock.unlock(); }
    }
    public State state() {
        State staged = transaction.get();
        if (staged == null) throw new IllegalStateException("Repository and gateway access requires a trading transaction");
        return staged;
    }
}
