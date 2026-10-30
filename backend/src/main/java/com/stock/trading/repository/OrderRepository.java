package com.stock.trading.repository;

import com.stock.trading.model.Order;
import com.stock.trading.enums.OrderStatus;
import com.stock.trading.exception.TradingException;
import com.stock.trading.store.TradingUnitOfWork;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private final TradingUnitOfWork work;
    public OrderRepository(TradingUnitOfWork work) { this.work=work; }
    public Order find(UUID id) {
        Order order=work.state().orders.get(id);
        if(order==null) throw TradingException.notFound("Order not found");
        return order;
    }
    public void save(Order order) { work.state().orders.put(order.id(),order); }
    public List<Order> byUser(UUID userId) {
        return work.state().orders.values().stream().filter(o->o.userId().equals(userId))
                .sorted(Comparator.comparing(Order::createdAt).reversed().thenComparing(Order::id)).toList();
    }
    public List<Order> pending() {
        return work.state().orders.values().stream().filter(o->o.status()==OrderStatus.PENDING)
                .sorted(Comparator.comparing(Order::createdAt).thenComparing(Order::id)).toList();
    }
    public long pendingCount(UUID userId) {
        return work.state().orders.values().stream().filter(o->o.userId().equals(userId)&&o.status()==OrderStatus.PENDING).count();
    }
}
