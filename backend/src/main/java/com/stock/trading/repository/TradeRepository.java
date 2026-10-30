package com.stock.trading.repository;

import com.stock.trading.model.Trade;
import com.stock.trading.store.TradingUnitOfWork;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class TradeRepository {
    private final TradingUnitOfWork work;
    public TradeRepository(TradingUnitOfWork work) { this.work=work; }
    public void save(Trade trade) {
        if(work.state().trades.values().stream().anyMatch(t->t.orderId().equals(trade.orderId())))
            throw new IllegalStateException("An execution already exists for this order");
        work.state().trades.put(trade.id(),trade);
    }
    public List<Trade> byUser(UUID userId) {
        return work.state().trades.values().stream().filter(t->t.userId().equals(userId))
                .sorted(Comparator.comparing(Trade::executedAt).reversed().thenComparing(Trade::id)).toList();
    }
}
