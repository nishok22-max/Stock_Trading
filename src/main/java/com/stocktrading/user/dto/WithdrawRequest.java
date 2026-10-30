package com.stocktrading.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for Withdrawal request.
 */
public class WithdrawRequest {

    private String userId;
    private String accountId;

    @NotNull(message = "Withdrawal amount is required")
    @DecimalMin(value = "1.00", message = "Withdrawal amount must be at least Rs. 1")
    private BigDecimal amount;

    private String bankAccountNumber;
    private String description;

    public WithdrawRequest() {
    }

    public WithdrawRequest(String userId, BigDecimal amount, String bankAccountNumber, String description) {
        this.userId = userId;
        this.amount = amount;
        this.bankAccountNumber = bankAccountNumber;
        this.description = description;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

