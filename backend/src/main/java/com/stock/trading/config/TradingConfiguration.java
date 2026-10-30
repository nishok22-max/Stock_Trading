package com.stock.trading.config;

import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration
public class TradingConfiguration {
    @Bean public Clock tradingClock() { return Clock.systemUTC(); }
}
