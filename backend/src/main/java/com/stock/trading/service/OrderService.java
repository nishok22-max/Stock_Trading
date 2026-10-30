package com.stock.trading.service;

import com.stock.trading.dto.OrderResponse;
import com.stock.trading.enums.OrderStatus;
import com.stock.trading.exception.TradingException;
import com.stock.trading.port.AccountGateway;
import com.stock.trading.repository.*;
import com.stock.trading.model.Trade;
import com.stock.trading.store.TradingUnitOfWork;
import java.time.Clock;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    private final TradingUnitOfWork work;
    private final OrderRepository orders;
    private final TradeRepository trades;
    private final AccountGateway accounts;
    private final OrderExecutionService execution;
    private final Clock clock;
    public OrderService(TradingUnitOfWork work, OrderRepository orders, TradeRepository trades, AccountGateway accounts, OrderExecutionService execution, Clock clock) {
        this.work=work; this.orders=orders; this.trades=trades; this.accounts=accounts; this.execution=execution; this.clock=clock;
    }
    public OrderResponse get(UUID actor,UUID id) {
        return work.execute(()->{
            var order=orders.find(id);
            if(!order.userId().equals(actor)) throw TradingException.notFound("Order not found");
            return OrderResponse.from(order);
        });
    }
    public List<OrderResponse> history(UUID actor,UUID userId) {
        own(actor,userId);
        return work.execute(()->{ accounts.account(userId); return orders.byUser(userId).stream().map(OrderResponse::from).toList(); });
    }
    public List<Trade> trades(UUID actor,UUID userId) {
        own(actor,userId);
        return work.execute(()->{ accounts.account(userId); return trades.byUser(userId); });
    }
    public OrderResponse cancel(UUID actor,UUID id) {
        return work.execute(()->{
            var order=orders.find(id);
            if(!order.userId().equals(actor)) throw TradingException.notFound("Order not found");
            if(order.status()!=OrderStatus.PENDING) throw TradingException.conflict("Only pending orders can be cancelled");
            var now=clock.instant();
            return OrderResponse.from(execution.close(order,OrderStatus.CANCELLED,
                    now.isBefore(order.expiresAt())?"USER_CANCELLED: Cancelled by user":"EXPIRED: Market session ended",now));
        });
    }
    private void own(UUID actor,UUID userId) {
        if(!actor.equals(userId)) throw new TradingException("FORBIDDEN","You can access only your own trading history",HttpStatus.FORBIDDEN);
    }
}
