package com.stock.trading.model;

import java.time.Instant;

public record MarketStatus(boolean open, Instant serverTime, Instant sessionClose, Instant nextOpeningTime) {}
