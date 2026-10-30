package com.stock.trading.service;

import com.stock.trading.model.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class BrokerageService {
    public record Charges(BigDecimal value, BigDecimal brokerage, BigDecimal gst, BigDecimal totalCharges, BigDecimal buyTotal, BigDecimal sellProceeds) {}
    public Charges calculate(BigDecimal price, int quantity) {
        BigDecimal value=Money.value(price,quantity);
        BigDecimal brokerage=Money.amount(value.multiply(new BigDecimal("0.0005")).min(new BigDecimal("20.00")));
        BigDecimal gst=Money.amount(brokerage.multiply(new BigDecimal("0.18")));
        BigDecimal total=brokerage.add(gst);
        return new Charges(value,brokerage,gst,total,value.add(total),value.subtract(total));
    }
}
