package com.stock.trading.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
    public record ApiError(String code,String message,int status,String path,Instant timestamp) {}
    @ExceptionHandler(TradingException.class)
    public ResponseEntity<ApiError> business(TradingException ex,HttpServletRequest request) { return error(ex.status(),ex.code(),ex.getMessage(),request); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex,HttpServletRequest request) {
        String message=ex.getBindingResult().getAllErrors().stream().map(e->e.getDefaultMessage()).distinct().sorted().reduce((a,b)->a+"; "+b).orElse("Invalid request");
        return error(HttpStatus.BAD_REQUEST,"INVALID_REQUEST",message,request);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class,MissingRequestHeaderException.class,MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> malformed(Exception ex,HttpServletRequest request) { return error(HttpStatus.BAD_REQUEST,"INVALID_REQUEST","Provide valid JSON, enum values, UUIDs, required X-User-Id, and a whole-number quantity",request); }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> missing(NoResourceFoundException ex,HttpServletRequest request) { return error(HttpStatus.NOT_FOUND,"NOT_FOUND","Endpoint not found",request); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(HttpRequestMethodNotSupportedException ex,HttpServletRequest request) { return error(HttpStatus.METHOD_NOT_ALLOWED,"METHOD_NOT_ALLOWED","HTTP method is not supported for this endpoint",request); }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception ex,HttpServletRequest request) {
        log.error("Trading request failed; any staged changes were discarded",ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,"INTERNAL_ERROR","The operation could not be completed. Retry placement with the same Idempotency-Key.",request);
    }
    private ResponseEntity<ApiError> error(HttpStatus status,String code,String message,HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(code,message,status.value(),request.getRequestURI(),Instant.now()));
    }
}
