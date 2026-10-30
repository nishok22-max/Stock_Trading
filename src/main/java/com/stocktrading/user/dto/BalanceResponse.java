package com.stocktrading.user.dto;

import com.stocktrading.user.model.AccountStatus;
import com.stocktrading.user.model.KycStatus;

import java.math.BigDecimal;

/**
 * DTO for Balance Enquiry response.
 */
public class BalanceResponse {
    private String userId;
    private String accountId;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private BigDecimal totalBalance;
    private AccountStatus status;
    private KycStatus kycStatus;
    private boolean canTrade;

    public BalanceResponse() {
    }

    public BalanceResponse(String userId, String accountId, BigDecimal availableBalance, 
                           BigDecimal blockedBalance, BigDecimal totalBalance, 
                           AccountStatus status, KycStatus kycStatus, boolean canTrade) {
        this.userId = userId;
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.blockedBalance = blockedBalance;
        this.totalBalance = totalBalance;
        this.status = status;
        this.kycStatus = kycStatus;
        this.canTrade = canTrade;
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

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getBlockedBalance() {
        return blockedBalance;
    }

    public void setBlockedBalance(BigDecimal blockedBalance) {
        this.blockedBalance = blockedBalance;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
    }

    public boolean isCanTrade() {
        return canTrade;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }
}
