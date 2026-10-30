package com.stock.trading.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="trading.scheduler-enabled",havingValue="true",matchIfMissing=true)
public class PendingOrderScheduler {
    private final TradingService trading;
    public PendingOrderScheduler(TradingService trading) { this.trading=trading; }
    @Scheduled(fixedDelayString="${trading.poll-delay-ms:1000}")
    public void process() { trading.processPending(); }
}
