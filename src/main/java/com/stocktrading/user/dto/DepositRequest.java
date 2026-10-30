package com.stocktrading.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO for Deposit request.
 * Min Rs. 100, Max Rs. 1,00,000 per transaction.
 */
public class DepositRequest {

    private String userId;
    private String accountId;

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "100.00", message = "Minimum deposit amount is Rs. 100")
    @DecimalMax(value = "100000.00", message = "Maximum deposit amount is Rs. 1,00,000 per transaction")
    private BigDecimal amount;

    private String bankAccountNumber;
    private String description;

    public DepositRequest() {
    }

    public DepositRequest(String userId, BigDecimal amount, String bankAccountNumber, String description) {
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
