package com.stocktrading.user.exception;

public class AccountBlockedException extends RuntimeException {
    public AccountBlockedException(String message) {
        super(message);
    }
}

