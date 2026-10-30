package com.stock.trading.service;

import com.stock.trading.dto.*;
import com.stock.trading.enums.*;
import com.stock.trading.exception.TradingException;
import com.stock.trading.model.*;
import com.stock.trading.port.*;
import com.stock.trading.repository.OrderRepository;
import com.stock.trading.store.TradingUnitOfWork;
import com.stock.trading.store.TradingUnitOfWork.*;
import jakarta.validation.Validator;
import java.time.*;
import java.util.*;
import org.slf4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TradingService {
    private static final Logger log=LoggerFactory.getLogger(TradingService.class);
    private final TradingUnitOfWork work;
    private final AccountGateway accounts;
    private final MarketGateway market;
    private final OrderRepository orders;
    private final BrokerageService brokerage;
    private final OrderExecutionService execution;
    private final Clock clock;
    private final Validator validator;
    public TradingService(TradingUnitOfWork work, AccountGateway accounts, MarketGateway market, OrderRepository orders,
                          BrokerageService brokerage, OrderExecutionService execution, Clock clock, Validator validator) {
        this.work=work; this.accounts=accounts; this.market=market; this.orders=orders; this.brokerage=brokerage;
        this.execution=execution; this.clock=clock; this.validator=validator;
    }
    public OrderResponse place(UUID userId, PlaceOrderRequest request, String key) {
        validateRequest(request);
        if(key!=null && !key.matches("[A-Za-z0-9._:-]{1,128}"))
            throw new TradingException("INVALID_IDEMPOTENCY_KEY","Idempotency-Key must contain 1–128 letters, digits, dots, underscores, colons or hyphens",HttpStatus.BAD_REQUEST);
        return work.execute(()->{
            accounts.account(userId);
            String fingerprint=fingerprint(request);
            var requestKey=new RequestKey(userId,key);
            var prior=key==null ? null : work.state().requests.get(requestKey);
            if(prior!=null) {
                if(!prior.fingerprint().equals(fingerprint)) throw TradingException.conflict("Idempotency-Key was already used for a different order");
                return OrderResponse.from(orders.find(prior.orderId()));
            }
            Instant now=clock.instant();
            expireUser(userId,now);
            var order=Order.pending(userId,request.normalizedSymbol(),request.side(),request.type(),request.quantity(),
                    request.limitPrice()==null ? null : Money.amount(request.limitPrice()),now,market.sessionClose(now));
            OrderPreview preview;
            try { preview=previewInside(userId,request,now); }
            catch(TradingException rejected) {
                var result=order.closed(OrderStatus.REJECTED,rejected.code()+": "+rejected.getMessage(),now);
                orders.save(result); remember(requestKey,key,fingerprint,result);
                return OrderResponse.from(result);
            }
            // Exceptions after validation propagate and roll back the entire transaction.
            if(request.side()==OrderSide.BUY) accounts.reserveCash(userId,preview.cashToReserve());
            else accounts.reserveShares(userId,order.symbol(),request.quantity());
            order=order.reserved(preview.cashToReserve(),preview.sharesToReserve());
            orders.save(order);
            if(preview.executesImmediately()) order=execution.execute(order,preview.currentPrice(),now);
            remember(requestKey,key,fingerprint,order);
            return OrderResponse.from(order);
        });
    }
    public OrderPreview preview(UUID userId, PlaceOrderRequest request) {
        validateRequest(request);
        return work.execute(()->{
            Instant now=clock.instant();
            expireUser(userId,now);
            return previewInside(userId,request,now);
        });
    }
    private OrderPreview previewInside(UUID userId, PlaceOrderRequest r, Instant now) {
        var account=accounts.account(userId);
        var stock=market.quote(r.normalizedSymbol());
        eligibility(account,stock,r.side());
        if(!market.isOpen(now)) throw TradingException.rule("MARKET_CLOSED","Market is open Monday–Friday, 09:15–15:30 IST");
        boolean immediate=ExecutionRule.forType(r.type()).matches(r.side(),stock.price(),r.limitPrice());
        if(!immediate && orders.pendingCount(userId)>=25) throw TradingException.rule("PENDING_LIMIT","You can have at most 25 pending orders");
        var reservationPrice=r.type()==OrderType.LIMIT ? r.limitPrice() : stock.price();
        var charges=brokerage.calculate(reservationPrice,r.quantity());
        var cash=r.side()==OrderSide.BUY ? charges.buyTotal() : Money.ZERO;
        if(r.side()==OrderSide.BUY && account.availableBalance().compareTo(cash)<0)
            throw TradingException.rule("INSUFFICIENT_FUNDS","Available balance does not cover the order and charges");
        if(r.side()==OrderSide.SELL && accounts.holding(userId,stock.symbol()).availableQuantity()<r.quantity())
            throw TradingException.rule("INSUFFICIENT_SHARES","Not enough unreserved shares to sell");
        return new OrderPreview(r,stock.price(),reservationPrice,charges,cash,r.side()==OrderSide.SELL?r.quantity():0,immediate,now,market.sessionClose(now));
    }
    public void processPending() {
        var ids=work.execute(()->orders.pending().stream().map(Order::id).toList());
        for(var id:ids) {
            try { processOrder(id); }
            catch(RuntimeException failure) { log.error("Pending order {} processing failed; transaction rolled back",id,failure); }
        }
    }
    public void processOrder(UUID id) {
        work.execute(()->{
            var order=orders.find(id);
            if(order.status()!=OrderStatus.PENDING) return null;
            Instant now=clock.instant();
            if(!now.isBefore(order.expiresAt())) { execution.close(order,OrderStatus.CANCELLED,"EXPIRED: Market session ended",now); return null; }
            if(!market.isOpen(now)) return null;
            StockQuote quote;
            try {
                quote=market.quote(order.symbol());
                eligibility(accounts.account(order.userId()),quote,order.side());
            } catch(TradingException rejected) {
                execution.close(order,OrderStatus.REJECTED,rejected.code()+": "+rejected.getMessage(),now);
                return null;
            }
            if(order.canExecute(quote.price())) execution.execute(order,quote.price(),now);
            return null;
        });
    }
    private void expireUser(UUID userId, Instant now) {
        for(var order:orders.byUser(userId)) if(order.status()==OrderStatus.PENDING && !now.isBefore(order.expiresAt()))
            execution.close(order,OrderStatus.CANCELLED,"EXPIRED: Market session ended",now);
    }
    private void eligibility(AccountSnapshot account, StockQuote stock, OrderSide side) {
        if(!account.kycApproved()) throw TradingException.rule("KYC_REQUIRED","KYC must be approved before trading");
        if(account.blocked() && side==OrderSide.BUY) throw TradingException.rule("ACCOUNT_BLOCKED","Blocked accounts cannot place buy orders");
        if(!stock.active()) throw TradingException.rule("STOCK_SUSPENDED","Suspended stocks cannot be traded");
        if(stock.price()==null || stock.price().signum()<=0 || stock.price().scale()>2) throw new IllegalStateException("Market service returned an invalid quote");
    }
    private void validateRequest(PlaceOrderRequest r) {
        if(r==null || !validator.validate(r).isEmpty()) throw new TradingException("INVALID_REQUEST","Use a valid symbol, side, type, whole quantity from 1 to 1000, and a positive two-decimal price for LIMIT orders only",HttpStatus.BAD_REQUEST);
    }
    private String fingerprint(PlaceOrderRequest r) {
        return r.normalizedSymbol()+"|"+r.side()+"|"+r.type()+"|"+r.quantity()+"|"+(r.limitPrice()==null?"":Money.amount(r.limitPrice()).toPlainString());
    }
    private void remember(RequestKey requestKey,String key,String fingerprint,Order order) {
        if(key!=null) work.state().requests.put(requestKey,new IdempotencyEntry(fingerprint,order.id()));
    }
}
