package com.stocktrading.user.controller;

import com.stocktrading.user.dto.AccountResponse;
import com.stocktrading.user.dto.ApiResponse;
import com.stocktrading.user.dto.BalanceResponse;
import com.stocktrading.user.dto.DepositRequest;
import com.stocktrading.user.dto.WithdrawRequest;
import com.stocktrading.user.model.Transaction;
import com.stocktrading.user.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Account Management and Financial Transactions.
 * Endpoints:
 * - GET  /api/account/{userId}              : Get account
 * - GET  /api/account/{userId}/balance      : Check balance
 * - POST /api/account/deposit               : Deposit
 * - POST /api/account/withdraw              : Withdraw
 * - GET  /api/account/{userId}/transactions : Transaction history
 */
@RestController
@RequestMapping("/api/account")
@CrossOrigin(origins = "*")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Get trading account details by user ID.
     * GET /api/account/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String userId) {
        AccountResponse response = accountService.getAccountResponseByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check account balance enquiry.
     * GET /api/account/{userId}/balance
     */
    @GetMapping("/{userId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String userId) {
        BalanceResponse response = accountService.getBalanceByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Deposit funds into trading account.
     * POST /api/account/deposit
     */
    @PostMapping("/deposit")
    public ResponseEntity<AccountResponse> deposit(@Valid @RequestBody DepositRequest request) {
        AccountResponse response = accountService.deposit(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Withdraw funds from trading account.
     * POST /api/account/withdraw
     */
    @PostMapping("/withdraw")
    public ResponseEntity<AccountResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        AccountResponse response = accountService.withdraw(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction history for user.
     * GET /api/account/{userId}/transactions
     */
    @GetMapping("/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable String userId) {
        List<Transaction> transactions = accountService.getTransactionHistory(userId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Admin action: Approve KYC for user.
     * POST /api/account/{userId}/kyc/approve
     */
    @PostMapping("/{userId}/kyc/approve")
    public ResponseEntity<AccountResponse> approveKyc(@PathVariable String userId) {
        AccountResponse response = accountService.approveKyc(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Admin action: Toggle Block/Active status for account.
     * POST /api/account/{userId}/toggle-block
     */
    @PostMapping("/{userId}/toggle-block")
    public ResponseEntity<AccountResponse> toggleAccountBlock(@PathVariable String userId) {
        AccountResponse response = accountService.toggleAccountBlock(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's stock watchlist.
     * GET /api/account/{userId}/watchlist
     */
    @GetMapping("/{userId}/watchlist")
    public ResponseEntity<List<String>> getWatchlist(@PathVariable String userId) {
        List<String> watchlist = accountService.getWatchlist(userId);
        return ResponseEntity.ok(watchlist);
    }

    /**
     * Add stock symbol to user's watchlist (max 20).
     * POST /api/account/{userId}/watchlist/add
     */
    @PostMapping("/{userId}/watchlist/add")
    public ResponseEntity<List<String>> addToWatchlist(
            @PathVariable String userId, 
            @RequestBody Map<String, String> body) {
        String symbol = body.get("symbol");
        List<String> updated = accountService.addToWatchlist(userId, symbol);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remove stock symbol from user's watchlist.
     * POST /api/account/{userId}/watchlist/remove
     */
    @PostMapping("/{userId}/watchlist/remove")
    public ResponseEntity<List<String>> removeFromWatchlist(
            @PathVariable String userId, 
            @RequestBody Map<String, String> body) {
        String symbol = body.get("symbol");
        List<String> updated = accountService.removeFromWatchlist(userId, symbol);
        return ResponseEntity.ok(updated);
    }
}
