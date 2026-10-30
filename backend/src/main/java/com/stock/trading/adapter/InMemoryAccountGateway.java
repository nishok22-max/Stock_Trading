package com.stock.trading.adapter;

import com.stock.trading.port.*;
import com.stock.trading.model.Money;
import com.stock.trading.exception.TradingException;
import com.stock.trading.store.TradingUnitOfWork;
import com.stock.trading.store.TradingUnitOfWork.HoldingKey;
import java.math.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAccountGateway implements AccountGateway {
    private final TradingUnitOfWork work;
    public InMemoryAccountGateway(TradingUnitOfWork work) { this.work=work; }
    public AccountSnapshot account(UUID userId) {
        var account=work.state().accounts.get(userId);
        if(account==null) throw TradingException.notFound("Trading account not found");
        return account;
    }
    public HoldingSnapshot holding(UUID userId, String symbol) {
        return work.state().holdings.getOrDefault(new HoldingKey(userId,symbol),new HoldingSnapshot(userId,symbol,0,0,Money.ZERO));
    }
    public List<HoldingSnapshot> holdings(UUID userId) {
        return work.state().holdings.values().stream().filter(h->h.userId().equals(userId)).sorted(Comparator.comparing(HoldingSnapshot::symbol)).toList();
    }
    public void reserveCash(UUID userId, BigDecimal amount) {
        var a=account(userId);
        if(a.availableBalance().compareTo(amount)<0) throw TradingException.rule("INSUFFICIENT_FUNDS","Available balance does not cover the order and charges");
        balances(a,a.availableBalance().subtract(amount),a.blockedBalance().add(amount));
    }
    public void releaseCash(UUID userId, BigDecimal amount) {
        var a=account(userId);
        balances(a,a.availableBalance().add(amount),a.blockedBalance().subtract(amount));
    }
    public void reserveShares(UUID userId, String symbol, int quantity) {
        var h=holding(userId,symbol);
        if(h.availableQuantity()<quantity) throw TradingException.rule("INSUFFICIENT_SHARES","Not enough unreserved shares to sell");
        saveHolding(new HoldingSnapshot(userId,symbol,h.quantity(),h.reservedQuantity()+quantity,h.averageBuyPrice()));
    }
    public void releaseShares(UUID userId, String symbol, int quantity) {
        var h=holding(userId,symbol);
        saveHolding(new HoldingSnapshot(userId,symbol,h.quantity(),h.reservedQuantity()-quantity,h.averageBuyPrice()));
    }
    public void settleBuy(UUID userId, String symbol, int quantity, BigDecimal price, BigDecimal reservation, BigDecimal charges) {
        BigDecimal cost=Money.value(price,quantity).add(charges);
        if(cost.compareTo(reservation)>0) throw new IllegalStateException("Execution exceeds cash reservation");
        var a=account(userId);
        balances(a,a.availableBalance().add(reservation.subtract(cost)),a.blockedBalance().subtract(reservation));
        var h=holding(userId,symbol);
        BigDecimal average=h.averageBuyPrice().multiply(BigDecimal.valueOf(h.quantity())).add(price.multiply(BigDecimal.valueOf(quantity)))
                .divide(BigDecimal.valueOf((long)h.quantity()+quantity),8,RoundingMode.HALF_UP);
        saveHolding(new HoldingSnapshot(userId,symbol,Math.addExact(h.quantity(),quantity),h.reservedQuantity(),average));
    }
    public BigDecimal settleSell(UUID userId, String symbol, int quantity, BigDecimal price, BigDecimal charges) {
        var h=holding(userId,symbol);
        if(h.quantity()<quantity || h.reservedQuantity()<quantity) throw new IllegalStateException("Sell reservation is missing");
        var a=account(userId);
        balances(a,a.availableBalance().add(Money.value(price,quantity)).subtract(charges),a.blockedBalance());
        saveHolding(new HoldingSnapshot(userId,symbol,h.quantity()-quantity,h.reservedQuantity()-quantity,h.averageBuyPrice()));
        return Money.amount(price.subtract(h.averageBuyPrice()).multiply(BigDecimal.valueOf(quantity)));
    }
    private void balances(AccountSnapshot a, BigDecimal available, BigDecimal blocked) {
        if(available.signum()<0 || blocked.signum()<0) throw new IllegalStateException("Account balances cannot be negative");
        work.state().accounts.put(a.userId(),new AccountSnapshot(a.userId(),a.name(),a.kycApproved(),a.blocked(),Money.amount(available),Money.amount(blocked)));
    }
    private void saveHolding(HoldingSnapshot h) {
        if(h.quantity()<0 || h.reservedQuantity()<0 || h.reservedQuantity()>h.quantity()) throw new IllegalStateException("Invalid holding reservation");
        var key=new HoldingKey(h.userId(),h.symbol());
        if(h.quantity()==0) work.state().holdings.remove(key); else work.state().holdings.put(key,h);
    }
}
