package com.stocktrading.user.model;

import com.stocktrading.user.exception.AccountBlockedException;
import com.stocktrading.user.exception.InsufficientBalanceException;
import com.stocktrading.user.exception.InvalidTransactionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Account model representing a user's trading account and fund balances.
 * Follows business rules:
 * - Funds split into Available and Blocked.
 * - Min deposit Rs. 100, Max deposit Rs. 1,00,000 per transaction.
 * - Withdraw only from Available funds; balance never negative.
 * - Maximum 20 stocks in watchlist.
 * - Admin can block account.
 */
public class Account {
    public static final BigDecimal MIN_DEPOSIT = new BigDecimal("100.00");
    public static final BigDecimal MAX_DEPOSIT = new BigDecimal("100000.00");
    public static final int MAX_WATCHLIST_SIZE = 20;

    private String accountId;
    private String userId;
    private BigDecimal availableBalance;
    private BigDecimal blockedBalance;
    private AccountStatus status;
    private KycStatus kycStatus;
    private String linkedBankAccount;
    private List<String> watchlist;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {
        this.availableBalance = BigDecimal.ZERO;
        this.blockedBalance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.kycStatus = KycStatus.PENDING;
        this.watchlist = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Account(String accountId, String userId, BigDecimal initialBalance, 
                   String linkedBankAccount, KycStatus kycStatus) {
        this.accountId = accountId;
        this.userId = userId;
        this.availableBalance = (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) >= 0) 
                                ? initialBalance : BigDecimal.ZERO;
        this.blockedBalance = BigDecimal.ZERO;
        this.status = AccountStatus.ACTIVE;
        this.kycStatus = kycStatus != null ? kycStatus : KycStatus.PENDING;
        this.linkedBankAccount = linkedBankAccount;
        this.watchlist = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Encapsulation: Getters and Setters
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
        return availableBalance.add(blockedBalance);
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public KycStatus getKycStatus() {
        return kycStatus;
    }

    public void setKycStatus(KycStatus kycStatus) {
        this.kycStatus = kycStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLinkedBankAccount() {
        return linkedBankAccount;
    }

    public void setLinkedBankAccount(String linkedBankAccount) {
        this.linkedBankAccount = linkedBankAccount;
    }

    public List<String> getWatchlist() {
        return Collections.unmodifiableList(watchlist);
    }

    public void setWatchlist(List<String> watchlist) {
        this.watchlist = watchlist != null ? new ArrayList<>(watchlist) : new ArrayList<>();
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

    // Business Methods

    /**
     * Add funds from linked bank account.
     * Rule: Min Rs. 100, Max Rs. 1,00,000 per transaction.
     */
    public synchronized void deposit(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidTransactionException("Deposit amount cannot be null");
        }
        if (amount.compareTo(MIN_DEPOSIT) < 0) {
            throw new InvalidTransactionException("Minimum deposit amount is Rs. " + MIN_DEPOSIT);
        }
        if (amount.compareTo(MAX_DEPOSIT) > 0) {
            throw new InvalidTransactionException("Maximum deposit amount per transaction is Rs. " + MAX_DEPOSIT);
        }
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Withdraw available funds to linked bank account.
     * Rule: Can withdraw only Available funds. Balance cannot be negative.
     */
    public synchronized void withdraw(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }
        if (this.status == AccountStatus.BLOCKED) {
            throw new AccountBlockedException("Account is blocked. Withdrawals are currently disabled.");
        }
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient available balance. Requested: Rs. " 
                    + amount + ", Available: Rs. " + this.availableBalance);
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Block funds when a buy order is placed.
     */
    public synchronized void blockFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount to block must be greater than zero");
        }
        if (this.status == AccountStatus.BLOCKED) {
            throw new AccountBlockedException("Account is blocked. Cannot place buy orders or block funds.");
        }
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient available balance to block funds. Required: Rs. " 
                    + amount + ", Available: Rs. " + this.availableBalance);
        }
        this.availableBalance = this.availableBalance.subtract(amount);
        this.blockedBalance = this.blockedBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Release funds when a buy order is cancelled.
     */
    public synchronized void releaseFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount to release must be greater than zero");
        }
        if (this.blockedBalance.compareTo(amount) < 0) {
            throw new InvalidTransactionException("Cannot release Rs. " + amount + " as only Rs. " 
                    + this.blockedBalance + " is blocked");
        }
        this.blockedBalance = this.blockedBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Deduct blocked funds when order executes.
     */
    public synchronized void deductBlockedFunds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Amount to deduct must be greater than zero");
        }
        if (this.blockedBalance.compareTo(amount) < 0) {
            throw new InvalidTransactionException("Cannot deduct Rs. " + amount + " as only Rs. " 
                    + this.blockedBalance + " is blocked");
        }
        this.blockedBalance = this.blockedBalance.subtract(amount);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Add stock symbol to watchlist (up to 20 stocks).
     */
    public synchronized boolean addToWatchlist(String symbol) {
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new InvalidTransactionException("Stock symbol cannot be empty");
        }
        String upperSymbol = symbol.trim().toUpperCase();
        if (watchlist.contains(upperSymbol)) {
            return false;
        }
        if (watchlist.size() >= MAX_WATCHLIST_SIZE) {
            throw new InvalidTransactionException("Watchlist limit reached. Maximum allowed is " + MAX_WATCHLIST_SIZE + " stocks.");
        }
        boolean added = watchlist.add(upperSymbol);
        this.updatedAt = LocalDateTime.now();
        return added;
    }

    /**
     * Remove stock symbol from watchlist.
     */
    public synchronized boolean removeFromWatchlist(String symbol) {
        if (symbol == null) return false;
        boolean removed = watchlist.remove(symbol.trim().toUpperCase());
        if (removed) {
            this.updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    public boolean canTrade() {
        return this.kycStatus == KycStatus.APPROVED && this.status == AccountStatus.ACTIVE;
    }
}

