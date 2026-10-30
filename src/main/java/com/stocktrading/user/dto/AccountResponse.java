package com.stocktrading.user.dto;

import com.stocktrading.user.model.AccountStatus;
import com.stocktrading.user.model.KycStatus;
import com.stocktrading.user.model.UserRole;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Account responses.
 */
public class AccountResponse {
    private String accountId;
    private String userId;
    private String userName;
    private String userEmail;
    private String panNumber;
    private String phone;
    private UserRole role;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private BigDecimal totalBalance;
    private AccountStatus status;
    private KycStatus kycStatus;
    private String linkedBankAccount;
    private List<String> watchlist;
    private boolean canTrade;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AccountResponse() {
    }

    public AccountResponse(String accountId, String userId, String userName, String userEmail, 
                           String panNumber, String phone, UserRole role, 
                           BigDecimal availableBalance, BigDecimal blockedBalance, 
                           BigDecimal totalBalance, AccountStatus status, KycStatus kycStatus, 
                           String linkedBankAccount, List<String> watchlist, boolean canTrade, 
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.accountId = accountId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.panNumber = panNumber;
        this.phone = phone;
        this.role = role;
        this.availableBalance = availableBalance;
        this.blockedBalance = blockedBalance;
        this.totalBalance = totalBalance;
        this.status = status;
        this.kycStatus = kycStatus;
        this.linkedBankAccount = linkedBankAccount;
        this.watchlist = watchlist;
        this.canTrade = canTrade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
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

    public String getLinkedBankAccount() {
        return linkedBankAccount;
    }

    public void setLinkedBankAccount(String linkedBankAccount) {
        this.linkedBankAccount = linkedBankAccount;
    }

    public List<String> getWatchlist() {
        return watchlist;
    }

    public void setWatchlist(List<String> watchlist) {
        this.watchlist = watchlist;
    }

    public boolean isCanTrade() {
        return canTrade;
    }

    public void setCanTrade(boolean canTrade) {
        this.canTrade = canTrade;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
