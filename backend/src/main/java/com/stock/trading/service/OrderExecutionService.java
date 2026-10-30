package com.stock.trading.service;

import com.stock.trading.model.*;
import com.stock.trading.enums.*;
import com.stock.trading.port.*;
import com.stock.trading.repository.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Called only inside the shared transaction. No partial fills. */
@Service
public class OrderExecutionService {
    private final AccountGateway accounts;
    private final BrokerageService brokerage;
    private final OrderRepository orders;
    private final TradeRepository trades;
    public OrderExecutionService(AccountGateway accounts, BrokerageService brokerage, OrderRepository orders, TradeRepository trades) {
        this.accounts=accounts; this.brokerage=brokerage; this.orders=orders; this.trades=trades;
    }
    public Order execute(Order order, BigDecimal price, Instant now) {
        order=orders.find(order.id());
        if(!order.canExecute(price)) throw new IllegalStateException("Order cannot execute at this quote");
        var fee=brokerage.calculate(price,order.quantity());
        BigDecimal pnl=Money.ZERO;
        if(order.side()==OrderSide.BUY) accounts.settleBuy(order.userId(),order.symbol(),order.quantity(),price,order.reservedCash(),fee.totalCharges());
        else pnl=accounts.settleSell(order.userId(),order.symbol(),order.quantity(),price,fee.totalCharges());
        var trade=new Trade(UUID.randomUUID(),order.id(),order.userId(),order.symbol(),order.side(),order.quantity(),price,
                fee.value(),fee.brokerage(),fee.gst(),fee.totalCharges(),
                order.side()==OrderSide.BUY ? fee.buyTotal().negate() : fee.sellProceeds(),pnl,now);
        trades.save(trade);
        var executed=order.executed(trade);
        orders.save(executed);
        return executed;
    }
    public Order close(Order order, OrderStatus status, String reason, Instant now) {
        order=orders.find(order.id());
        // Guard before releasing funds or shares, including on repeated cancellation.
        if(order.status()!=OrderStatus.PENDING) throw new IllegalStateException("Only pending orders may close");
        if(order.reservedCash().signum()>0) accounts.releaseCash(order.userId(),order.reservedCash());
        if(order.reservedShares()>0) accounts.releaseShares(order.userId(),order.symbol(),order.reservedShares());
        var closed=order.closed(status,reason,now);
        orders.save(closed);
        return closed;
    }
}
