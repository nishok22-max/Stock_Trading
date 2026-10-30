package com.stocktrading.user.service;

import com.stocktrading.user.dto.AccountResponse;
import com.stocktrading.user.dto.BalanceResponse;
import com.stocktrading.user.dto.DepositRequest;
import com.stocktrading.user.dto.WithdrawRequest;
import com.stocktrading.user.exception.AccountBlockedException;
import com.stocktrading.user.exception.InsufficientBalanceException;
import com.stocktrading.user.exception.InvalidTransactionException;
import com.stocktrading.user.model.Account;
import com.stocktrading.user.model.AccountStatus;
import com.stocktrading.user.model.Customer;
import com.stocktrading.user.model.KycStatus;
import com.stocktrading.user.model.Transaction;
import com.stocktrading.user.model.TransactionType;
import com.stocktrading.user.repository.AccountRepository;
import com.stocktrading.user.repository.TransactionRepository;
import com.stocktrading.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AccountService Unit Tests")
class AccountServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        userRepository = new UserRepository();
        accountService = new AccountService(accountRepository, transactionRepository, userRepository);

        Customer testUser = new Customer(
                "USR-TEST-1",
                "Test User",
                "test@example.com",
                "hashedpassword",
                "ABCDE1234F",
                "9876543210",
                "HDFC-123456"
        );
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should create account with default zero balance and pending KYC")
    void testCreateAccount() {
        Account account = accountService.createAccount("USR-TEST-1", "HDFC-123456", null);

        assertNotNull(account);
        assertNotNull(account.getAccountId());
        assertEquals("USR-TEST-1", account.getUserId());
        assertEquals(BigDecimal.ZERO, account.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, account.getBlockedBalance());
        assertEquals(KycStatus.PENDING, account.getKycStatus());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    @DisplayName("Should successfully deposit valid amount between Rs. 100 and Rs. 1,00,000")
    void testDepositSuccess() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);

        DepositRequest request = new DepositRequest("USR-TEST-1", new BigDecimal("5000.00"), "HDFC-123456", "Salary deposit");
        AccountResponse response = accountService.deposit(request);

        assertEquals(new BigDecimal("5000.00"), response.getAvailableBalance());
        assertEquals(new BigDecimal("5000.00"), response.getTotalBalance());

        List<Transaction> transactions = accountService.getTransactionHistory("USR-TEST-1");
        assertEquals(1, transactions.size());
        assertEquals(TransactionType.DEPOSIT, transactions.get(0).getType());
        assertEquals(new BigDecimal("5000.00"), transactions.get(0).getAmount());
    }

    @Test
    @DisplayName("Should throw exception when deposit amount is less than Rs. 100")
    void testDepositBelowMinimum() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);

        DepositRequest request = new DepositRequest("USR-TEST-1", new BigDecimal("50.00"), "HDFC-123456", "Small deposit");
        assertThrows(InvalidTransactionException.class, () -> accountService.deposit(request));
    }

    @Test
    @DisplayName("Should throw exception when deposit amount exceeds Rs. 1,00,000")
    void testDepositAboveMaximum() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);

        DepositRequest request = new DepositRequest("USR-TEST-1", new BigDecimal("100001.00"), "HDFC-123456", "Excessive deposit");
        assertThrows(InvalidTransactionException.class, () -> accountService.deposit(request));
    }

    @Test
    @DisplayName("Should successfully withdraw available funds")
    void testWithdrawSuccess() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);
        accountService.deposit(new DepositRequest("USR-TEST-1", new BigDecimal("10000.00"), "HDFC-123456", "Deposit"));

        WithdrawRequest withdrawRequest = new WithdrawRequest("USR-TEST-1", new BigDecimal("4000.00"), "HDFC-123456", "Withdrawal");
        AccountResponse response = accountService.withdraw(withdrawRequest);

        assertEquals(new BigDecimal("6000.00"), response.getAvailableBalance());
        assertEquals(new BigDecimal("6000.00"), response.getTotalBalance());

        List<Transaction> transactions = accountService.getTransactionHistory("USR-TEST-1");
        assertEquals(2, transactions.size());
        assertEquals(TransactionType.WITHDRAWAL, transactions.get(0).getType());
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal exceeds available funds")
    void testWithdrawInsufficientBalance() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);
        accountService.deposit(new DepositRequest("USR-TEST-1", new BigDecimal("1000.00"), "HDFC-123456", "Deposit"));

        WithdrawRequest withdrawRequest = new WithdrawRequest("USR-TEST-1", new BigDecimal("1500.00"), "HDFC-123456", "Excessive withdrawal");
        assertThrows(InsufficientBalanceException.class, () -> accountService.withdraw(withdrawRequest));
    }

    @Test
    @DisplayName("Should throw AccountBlockedException when withdrawing from a blocked account")
    void testWithdrawFromBlockedAccount() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);
        accountService.deposit(new DepositRequest("USR-TEST-1", new BigDecimal("5000.00"), "HDFC-123456", "Deposit"));
        accountService.toggleAccountBlock("USR-TEST-1"); // Block account

        WithdrawRequest withdrawRequest = new WithdrawRequest("USR-TEST-1", new BigDecimal("1000.00"), "HDFC-123456", "Withdrawal");
        assertThrows(AccountBlockedException.class, () -> accountService.withdraw(withdrawRequest));
    }

    @Test
    @DisplayName("Should check balance correctly")
    void testBalanceEnquiry() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);
        accountService.deposit(new DepositRequest("USR-TEST-1", new BigDecimal("25000.00"), "HDFC-123456", "Deposit"));

        BalanceResponse balance = accountService.getBalanceByUserId("USR-TEST-1");
        assertEquals(new BigDecimal("25000.00"), balance.getAvailableBalance());
        assertEquals(BigDecimal.ZERO, balance.getBlockedBalance());
        assertEquals(new BigDecimal("25000.00"), balance.getTotalBalance());
    }

    @Test
    @DisplayName("Should manage watchlist and enforce 20 stocks limit")
    void testWatchlistEnforcement() {
        accountService.createAccount("USR-TEST-1", "HDFC-123456", null);

        for (int i = 1; i <= 20; i++) {
            accountService.addToWatchlist("USR-TEST-1", "STOCK" + i);
        }

        List<String> watchlist = accountService.getWatchlist("USR-TEST-1");
        assertEquals(20, watchlist.size());

        // Attempting to add 21st stock should throw InvalidTransactionException
        assertThrows(InvalidTransactionException.class, () -> 
            accountService.addToWatchlist("USR-TEST-1", "STOCK21")
        );

        // Removing a stock should reduce count
        accountService.removeFromWatchlist("USR-TEST-1", "STOCK1");
        assertEquals(19, accountService.getWatchlist("USR-TEST-1").size());
    }

    @Test
    @DisplayName("Should correctly block and release funds for order execution simulation")
    void testBlockAndReleaseFunds() {
        Account account = accountService.createAccount("USR-TEST-1", "HDFC-123456", null);
        account.deposit(new BigDecimal("10000.00"));

        account.blockFunds(new BigDecimal("3000.00"));
        assertEquals(new BigDecimal("7000.00"), account.getAvailableBalance());
        assertEquals(new BigDecimal("3000.00"), account.getBlockedBalance());
        assertEquals(new BigDecimal("10000.00"), account.getTotalBalance());

        account.releaseFunds(new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("8000.00"), account.getAvailableBalance());
        assertEquals(new BigDecimal("2000.00"), account.getBlockedBalance());
    }
}
