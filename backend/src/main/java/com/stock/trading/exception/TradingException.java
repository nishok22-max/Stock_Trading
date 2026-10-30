package com.stock.trading.exception;

import org.springframework.http.HttpStatus;

public class TradingException extends RuntimeException {
    private final String code;
    private final HttpStatus status;
    public TradingException(String code, String message, HttpStatus status) {
        super(message); this.code = code; this.status = status;
    }
    public String code() { return code; }
    public HttpStatus status() { return status; }
    public static TradingException rule(String code, String message) { return new TradingException(code,message,HttpStatus.UNPROCESSABLE_ENTITY); }
    public static TradingException notFound(String message) { return new TradingException("NOT_FOUND",message,HttpStatus.NOT_FOUND); }
    public static TradingException conflict(String message) { return new TradingException("CONFLICT",message,HttpStatus.CONFLICT); }
}
