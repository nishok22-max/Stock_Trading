package com.stocktrading.user.service;

import com.stocktrading.user.dto.AccountResponse;
import com.stocktrading.user.dto.LoginRequest;
import com.stocktrading.user.dto.LoginResponse;
import com.stocktrading.user.dto.RegisterRequest;
import com.stocktrading.user.exception.InvalidTransactionException;
import com.stocktrading.user.exception.UserAlreadyExistsException;
import com.stocktrading.user.exception.UserNotFoundException;
import com.stocktrading.user.repository.AccountRepository;
import com.stocktrading.user.repository.TransactionRepository;
import com.stocktrading.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private UserService userService;
    private AccountService accountService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository();
        accountRepository = new AccountRepository();
        transactionRepository = new TransactionRepository();
        userService = new UserService(userRepository);
        accountService = new AccountService(accountRepository, transactionRepository, userRepository);
        authService = new AuthService(userService, accountService);
    }

    @Test
    @DisplayName("Should successfully register a new user and create trading account")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "Kavita Roy",
                "kavita@example.com",
                "Secret@123",
                "ABCDE1234K",
                "9811223344",
                "SBI-998877",
                new BigDecimal("500.00")
        );

        AccountResponse response = authService.register(request);

        assertNotNull(response);
        assertNotNull(response.getUserId());
        assertNotNull(response.getAccountId());
        assertEquals("Kavita Roy", response.getUserName());
        assertEquals("kavita@example.com", response.getUserEmail());
        assertEquals(new BigDecimal("500.00"), response.getAvailableBalance());
    }

    @Test
    @DisplayName("Should reject registration with duplicate email")
    void testDuplicateEmail() {
        RegisterRequest request1 = new RegisterRequest(
                "User One", "dup@example.com", "Password123", "ABCDE1234A", "9800000001", "HDFC-1", null
        );
        authService.register(request1);

        RegisterRequest request2 = new RegisterRequest(
                "User Two", "dup@example.com", "Password123", "ABCDE1234B", "9800000002", "HDFC-2", null
        );

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request2));
    }

    @Test
    @DisplayName("Should reject registration with duplicate PAN number")
    void testDuplicatePan() {
        RegisterRequest request1 = new RegisterRequest(
                "User One", "user1@example.com", "Password123", "PANNO1234X", "9800000001", "HDFC-1", null
        );
        authService.register(request1);

        RegisterRequest request2 = new RegisterRequest(
                "User Two", "user2@example.com", "Password123", "PANNO1234X", "9800000002", "HDFC-2", null
        );

        assertThrows(UserAlreadyExistsException.class, () -> authService.register(request2));
    }

    @Test
    @DisplayName("Should successfully authenticate valid credentials")
    void testLoginSuccess() {
        RegisterRequest registerReq = new RegisterRequest(
                "Login Test", "logintest@example.com", "MyPassword123", "PANNO5678Y", "9800000003", "HDFC-3", null
        );
        authService.register(registerReq);

        LoginRequest loginReq = new LoginRequest("logintest@example.com", "MyPassword123");
        LoginResponse loginResponse = authService.login(loginReq);

        assertNotNull(loginResponse);
        assertNotNull(loginResponse.getToken());
        assertEquals("Login successful", loginResponse.getMessage());
        assertEquals("logintest@example.com", loginResponse.getEmail());
    }

    @Test
    @DisplayName("Should reject login with incorrect password")
    void testLoginIncorrectPassword() {
        RegisterRequest registerReq = new RegisterRequest(
                "Wrong Pass", "wrongpass@example.com", "CorrectPassword123", "PANNO9999Z", "9800000004", "HDFC-4", null
        );
        authService.register(registerReq);

        LoginRequest loginReq = new LoginRequest("wrongpass@example.com", "WrongPassword123");
        assertThrows(InvalidTransactionException.class, () -> authService.login(loginReq));
    }

    @Test
    @DisplayName("Should reject login for non-existent user")
    void testLoginNonExistentUser() {
        LoginRequest loginReq = new LoginRequest("nobody@example.com", "Password123");
        assertThrows(UserNotFoundException.class, () -> authService.login(loginReq));
    }
}

