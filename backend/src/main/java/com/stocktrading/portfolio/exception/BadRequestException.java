package com.stocktrading.portfolio.exception;

/**
 * Exception thrown when business rules or invariant validations are violated
 * (e.g. attempting to sell more shares than owned, duplicate watchlist additions, invalid quantities).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
