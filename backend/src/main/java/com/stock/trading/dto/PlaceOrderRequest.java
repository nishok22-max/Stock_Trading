package com.stock.trading.dto;

import com.stock.trading.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Locale;

public record PlaceOrderRequest(
        @NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9.-]{0,19}") String symbol,
        @NotNull OrderSide side, @NotNull OrderType type,
        @Min(1) @Max(1000) int quantity,
        @DecimalMin("0.01") @DecimalMax("999999999.99") @Digits(integer=9,fraction=2) BigDecimal limitPrice) {
    @JsonIgnore
    @AssertTrue(message="LIMIT orders require limitPrice; MARKET orders must omit limitPrice")
    public boolean isPriceCompatible() { return type==null || (type==OrderType.LIMIT ? limitPrice!=null : limitPrice==null); }
    public String normalizedSymbol() { return symbol.toUpperCase(Locale.ROOT); }
}
