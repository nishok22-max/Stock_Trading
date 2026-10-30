package com.stock.trading.model;

import com.stock.trading.enums.*;
import java.math.BigDecimal;

/** Polymorphic order pricing rules; immutable orders delegate behavior to a common abstraction. */
public abstract class ExecutionRule {
    private static final ExecutionRule MARKET=new MarketRule();
    private static final ExecutionRule LIMIT=new LimitRule();
    public static ExecutionRule forType(OrderType type) { return type==OrderType.MARKET ? MARKET : LIMIT; }
    public abstract boolean matches(OrderSide side,BigDecimal quote,BigDecimal limit);

    private static final class MarketRule extends ExecutionRule {
        @Override public boolean matches(OrderSide side,BigDecimal quote,BigDecimal limit) { return true; }
    }
    private static final class LimitRule extends ExecutionRule {
        @Override public boolean matches(OrderSide side,BigDecimal quote,BigDecimal limit) {
            return side==OrderSide.BUY ? quote.compareTo(limit)<=0 : quote.compareTo(limit)>=0;
        }
    }
}
