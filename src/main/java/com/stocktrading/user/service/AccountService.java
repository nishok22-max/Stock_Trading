package com.stocktrading.user.service;

import com.stocktrading.user.dto.AccountResponse;
import com.stocktrading.user.dto.BalanceResponse;
import com.stocktrading.user.dto.DepositRequest;
import com.stocktrading.user.dto.WithdrawRequest;
import com.stocktrading.user.exception.AccountBlockedException;
import com.stocktrading.user.exception.AccountNotFoundException;
import com.stocktrading.user.exception.InvalidTransactionException;
import com.stocktrading.user.exception.UserNotFoundException;
import com.stocktrading.user.model.Account;
import com.stocktrading.user.model.AccountStatus;
import com.stocktrading.user.model.KycStatus;
import com.stocktrading.user.model.Transaction;
import com.stocktrading.user.model.TransactionStatus;
import com.stocktrading.user.model.TransactionType;
import com.stocktrading.user.model.User;
import com.stocktrading.user.repository.AccountRepository;
import com.stocktrading.user.repository.TransactionRepository;
import com.stocktrading.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing trading accounts, deposits, withdrawals, balances, and transaction history.
 */
@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AtomicLong accountIdSequence = new AtomicLong(5100);
    private final AtomicLong transactionIdSequence = new AtomicLong(10000);

    public AccountService(AccountRepository accountRepository, 
                          TransactionRepository transactionRepository, 
                          UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public String generateAccountId() {
        return "ACC-" + accountIdSequence.incrementAndGet();
    }

    public String generateTransactionId() {
        return "TXN-" + transactionIdSequence.incrementAndGet();
    }

    /**
     * Create trading account for registered user.
     */
    public Account createAccount(String userId, String linkedBankAccount, BigDecimal initialDeposit) {
        String accountId = generateAccountId();
        Account account = new Account(
                accountId,
                userId,
                BigDecimal.ZERO,
                linkedBankAccount != null ? linkedBankAccount : "BANK-" + System.currentTimeMillis() % 1000000,
                KycStatus.PENDING
        );
        accountRepository.save(account);

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            deposit(new DepositRequest(userId, initialDeposit, linkedBankAccount, "Initial account funding"));
        }

        return account;
    }

    /**
     * Retrieve account by User ID.
     */
    public Account getAccountByUserId(String userId) {
        return accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AccountNotFoundException("Trading account not found for user ID: " + userId));
    }

    /**
     * Retrieve account by Account ID.
     */
    public Account getAccountById(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Trading account not found with ID: " + accountId));
    }

    /**
     * Retrieve full AccountResponse by User ID.
     */
    public AccountResponse getAccountResponseByUserId(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        Account account = getAccountByUserId(userId);
        return mapToAccountResponse(account, user);
    }

    /**
     * Check balance enquiry.
     */
    public BalanceResponse getBalanceByUserId(String userId) {
        Account account = getAccountByUserId(userId);
        return mapToBalanceResponse(account);
    }

    /**
     * Process fund deposit.
     * Rules: Rs. 100 <= amount <= Rs. 1,00,000 per transaction.
     */
    public AccountResponse deposit(DepositRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Deposit request cannot be null");
        }

        Account account = resolveAccount(request.getUserId(), request.getAccountId());
        User user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found for account: " + account.getAccountId()));

        BigDecimal amount = request.getAmount();
        try {
            account.deposit(amount);
            accountRepository.save(account);

            // Record transaction
            String desc = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                    ? request.getDescription() : "Funds deposited from linked bank account";
            Transaction txn = new Transaction(
                    generateTransactionId(),
                    account.getAccountId(),
                    account.getUserId(),
                    TransactionType.DEPOSIT,
                    amount,
                    account.getAvailableBalance(),
                    TransactionStatus.SUCCESS,
                    desc
            );
            transactionRepository.save(txn);

            return mapToAccountResponse(account, user);
        } catch (RuntimeException e) {
            Transaction failedTxn = new Transaction(
                    generateTransactionId(),
                    account.getAccountId(),
                    account.getUserId(),
                    TransactionType.DEPOSIT,
                    amount,
                    account.getAvailableBalance(),
                    TransactionStatus.FAILED,
                    "Deposit failed: " + e.getMessage()
            );
            transactionRepository.save(failedTxn);
            throw e;
        }
    }

    /**
     * Process fund withdrawal.
     * Rules: Available funds only. Balance cannot be negative. Account cannot be blocked.
     */
    public AccountResponse withdraw(WithdrawRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Withdrawal request cannot be null");
        }

        Account account = resolveAccount(request.getUserId(), request.getAccountId());
        User user = userRepository.findById(account.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found for account: " + account.getAccountId()));

        BigDecimal amount = request.getAmount();
        try {
            account.withdraw(amount);
            accountRepository.save(account);

            // Record transaction
            String desc = (request.getDescription() != null && !request.getDescription().trim().isEmpty())
                    ? request.getDescription() : "Funds withdrawn to linked bank account";
            Transaction txn = new Transaction(
                    generateTransactionId(),
                    account.getAccountId(),
                    account.getUserId(),
                    TransactionType.WITHDRAWAL,
                    amount,
                    account.getAvailableBalance(),
                    TransactionStatus.SUCCESS,
                    desc
            );
            transactionRepository.save(txn);

            return mapToAccountResponse(account, user);
        } catch (RuntimeException e) {
            Transaction failedTxn = new Transaction(
                    generateTransactionId(),
                    account.getAccountId(),
                    account.getUserId(),
                    TransactionType.WITHDRAWAL,
                    amount,
                    account.getAvailableBalance(),
                    TransactionStatus.FAILED,
                    "Withdrawal failed: " + e.getMessage()
            );
            transactionRepository.save(failedTxn);
            throw e;
        }
    }

    /**
     * Retrieve transaction history by User ID.
     */
    public List<Transaction> getTransactionHistory(String userId) {
        Account account = getAccountByUserId(userId);
        return transactionRepository.findByUserId(userId);
    }

    /**
     * Admin action: Approve KYC.
     */
    public AccountResponse approveKyc(String userId) {
        Account account = getAccountByUserId(userId);
        account.setKycStatus(KycStatus.APPROVED);
        accountRepository.save(account);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return mapToAccountResponse(account, user);
    }

    /**
     * Admin action: Block or Unblock Account.
     */
    public AccountResponse toggleAccountBlock(String userId) {
        Account account = getAccountByUserId(userId);
        if (account.getStatus() == AccountStatus.ACTIVE) {
            account.setStatus(AccountStatus.BLOCKED);
        } else {
            account.setStatus(AccountStatus.ACTIVE);
        }
        accountRepository.save(account);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return mapToAccountResponse(account, user);
    }

    /**
     * Add stock to watchlist (up to 20 stocks).
     */
    public List<String> addToWatchlist(String userId, String symbol) {
        Account account = getAccountByUserId(userId);
        account.addToWatchlist(symbol);
        accountRepository.save(account);
        return account.getWatchlist();
    }

    /**
     * Remove stock from watchlist.
     */
    public List<String> removeFromWatchlist(String userId, String symbol) {
        Account account = getAccountByUserId(userId);
        account.removeFromWatchlist(symbol);
        accountRepository.save(account);
        return account.getWatchlist();
    }

    /**
     * Get watchlist for user.
     */
    public List<String> getWatchlist(String userId) {
        Account account = getAccountByUserId(userId);
        return account.getWatchlist();
    }

    public Account save(Account account) {
        return accountRepository.save(account);
    }

    private Account resolveAccount(String userId, String accountId) {
        if (userId != null && !userId.trim().isEmpty()) {
            return getAccountByUserId(userId.trim());
        }
        if (accountId != null && !accountId.trim().isEmpty()) {
            return getAccountById(accountId.trim());
        }
        throw new InvalidTransactionException("Either userId or accountId must be provided");
    }

    public AccountResponse mapToAccountResponse(Account account, User user) {
        return new AccountResponse(
                account.getAccountId(),
                account.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getPanNumber(),
                user.getPhone(),
                user.getRole(),
                account.getAvailableBalance(),
                account.getBlockedBalance(),
                account.getTotalBalance(),
                account.getStatus(),
                account.getKycStatus(),
                account.getLinkedBankAccount(),
                account.getWatchlist(),
                account.canTrade(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public BalanceResponse mapToBalanceResponse(Account account) {
        return new BalanceResponse(
                account.getUserId(),
                account.getAccountId(),
                account.getAvailableBalance(),
                account.getBlockedBalance(),
                account.getTotalBalance(),
                account.getStatus(),
                account.getKycStatus(),
                account.canTrade()
        );
    }
}
