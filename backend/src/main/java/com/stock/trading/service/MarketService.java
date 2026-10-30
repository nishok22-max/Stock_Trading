package com.stock.trading.service;

import com.stock.trading.adapter.InMemoryMarketGateway;
import com.stock.trading.model.MarketStatus;
import com.stock.trading.port.MarketGateway;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import org.springframework.stereotype.Service;

@Service
public class MarketService {
    private static final LocalTime OPEN = LocalTime.of(9, 15);
    private final MarketGateway market;
    private final Clock clock;

    public MarketService(MarketGateway market, Clock clock) {
        this.market = market;
        this.clock = clock;
    }

    public MarketStatus status() {
        return status(clock.instant());
    }

    public MarketStatus status(Instant now) {
        boolean open = market.isOpen(now);
        return new MarketStatus(open, now, market.sessionClose(now), open ? null : nextOpening(now));
    }

    private Instant nextOpening(Instant now) {
        var local = now.atZone(InMemoryMarketGateway.ZONE);
        var date = local.toLocalDate();
        if (isWeekday(date.getDayOfWeek()) && local.toLocalTime().isBefore(OPEN))
            return date.atTime(OPEN).atZone(InMemoryMarketGateway.ZONE).toInstant();
        do {
            date = date.plusDays(1);
        } while (!isWeekday(date.getDayOfWeek()));
        return date.atTime(OPEN).atZone(InMemoryMarketGateway.ZONE).toInstant();
    }

    private boolean isWeekday(DayOfWeek day) {
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }
}
