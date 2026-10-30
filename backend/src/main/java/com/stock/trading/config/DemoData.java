package com.stock.trading.config;

import com.stock.trading.model.Money;
import com.stock.trading.port.*;
import com.stock.trading.store.TradingUnitOfWork;
import com.stock.trading.store.TradingUnitOfWork.HoldingKey;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;

@Configuration
@ConditionalOnProperty(name="trading.demo-enabled",havingValue="true",matchIfMissing=true)
public class DemoData {
    public static final UUID APPROVED=UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID BLOCKED=UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID UNAPPROVED=UUID.fromString("33333333-3333-4333-8333-333333333333");
    @Bean ApplicationRunner seedTradingDemo(TradingUnitOfWork work) {
        return args->work.execute(()->{
            work.state().accounts.put(APPROVED,new AccountSnapshot(APPROVED,"Aarav Mehta",true,false,new BigDecimal("150000.00"),Money.ZERO));
            work.state().accounts.put(BLOCKED,new AccountSnapshot(BLOCKED,"Mira Desai",true,true,new BigDecimal("25000.00"),Money.ZERO));
            work.state().accounts.put(UNAPPROVED,new AccountSnapshot(UNAPPROVED,"Rohan Iyer",false,false,new BigDecimal("25000.00"),Money.ZERO));
            for(var user:new UUID[]{APPROVED,BLOCKED}) {
                work.state().holdings.put(new HoldingKey(user,"TCS"),new HoldingSnapshot(user,"TCS",12,0,new BigDecimal("3420.00000000")));
                work.state().holdings.put(new HoldingKey(user,"INFY"),new HoldingSnapshot(user,"INFY",25,0,new BigDecimal("1480.00000000")));
            }
            seed(work,"TCS","Tata Consultancy Services","IT","3542.50","3510.00",true);
            seed(work,"INFY","Infosys Limited","IT","1524.80","1536.20",true);
            seed(work,"HDFCBANK","HDFC Bank","Banking","1682.35","1668.50",true);
            seed(work,"RELIANCE","Reliance Industries","Energy","2916.40","2894.70",true);
            seed(work,"SUNPHARMA","Sun Pharmaceutical","Pharma","1728.60","1712.30",true);
            seed(work,"TATAMOTORS","Tata Motors","Auto","948.25","957.10",true);
            seed(work,"HALTED","Suspended Demo Stock","IT","100.00","100.00",false);
            return null;
        });
    }
    private void seed(TradingUnitOfWork w,String symbol,String name,String sector,String price,String close,boolean active) {
        w.state().stocks.put(symbol,new StockQuote(symbol,name,sector,new BigDecimal(price),new BigDecimal(close),active));
    }
}
