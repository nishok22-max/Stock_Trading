package com.stocktrading.user.service;

import com.stocktrading.user.dto.AccountResponse;
import com.stocktrading.user.dto.LoginRequest;
import com.stocktrading.user.dto.LoginResponse;
import com.stocktrading.user.dto.RegisterRequest;
import com.stocktrading.user.exception.InvalidTransactionException;
import com.stocktrading.user.exception.UserNotFoundException;
import com.stocktrading.user.model.Account;
import com.stocktrading.user.model.Customer;
import com.stocktrading.user.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user authentication, registration, password hashing and active sessions.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final AccountService accountService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Map<String, String> activeTokens = new ConcurrentHashMap<>(); // token -> userId

    public AuthService(UserService userService, AccountService accountService) {
        this.userService = userService;
        this.accountService = accountService;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Register a new user with validation, password hashing, and trading account creation.
     */
    public AccountResponse register(RegisterRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Registration request cannot be null");
        }

        // 1. Password hashing
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 2. Create customer entity
        Customer customer = userService.createCustomer(request, encodedPassword);

        // 3. Create trading account with optional initial deposit
        Account account = accountService.createAccount(
                customer.getUserId(),
                customer.getBankAccountNumber(),
                request.getInitialDeposit()
        );

        return accountService.mapToAccountResponse(account, customer);
    }

    /**
     * Authenticate user credentials and return login response with active session token.
     */
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new InvalidTransactionException("Login request cannot be null");
        }

        // 1. Verify user exists
        User user = userService.getUserByEmail(request.getEmail());

        // 2. Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidTransactionException("Invalid email or password");
        }

        // 3. Generate session token
        String token = "TOKEN-" + UUID.randomUUID().toString();
        activeTokens.put(token, user.getUserId());

        // 4. Retrieve account response
        AccountResponse accountResponse = null;
        try {
            accountResponse = accountService.getAccountResponseByUserId(user.getUserId());
        } catch (Exception ignored) {
            // Admin users might not have trading accounts
        }

        return new LoginResponse(
                token,
                "Login successful",
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                accountResponse
        );
    }

    public String validateToken(String token) {
        if (token == null) return null;
        return activeTokens.get(token);
    }

    public void logout(String token) {
        if (token != null) {
            activeTokens.remove(token);
        }
    }

    public BCryptPasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
