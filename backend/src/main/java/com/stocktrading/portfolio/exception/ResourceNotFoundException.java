package com.stocktrading.portfolio.exception;

/**
 * Exception thrown when a requested resource (holding, watchlist item, portfolio) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
