package com.stocktrading.config;

import com.stocktrading.user.dto.DepositRequest;
import com.stocktrading.user.model.Account;
import com.stocktrading.user.model.AccountStatus;
import com.stocktrading.user.model.Customer;
import com.stocktrading.user.model.KycStatus;
import com.stocktrading.user.service.AccountService;
import com.stocktrading.user.service.AuthService;
import com.stocktrading.user.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Initializes in-memory seed data per problem statement requirements:
 * "You can hardcode a few users with some funds... as initial data to start the program."
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;
    private final AccountService accountService;
    private final AuthService authService;

    public DataInitializer(UserService userService, AccountService accountService, AuthService authService) {
        this.userService = userService;
        this.accountService = accountService;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        String hashedPassword = authService.getPasswordEncoder().encode("Password@123");

        // 1. Admin User
        userService.createAdmin(
                "System Admin",
                "admin@stocktrade.com",
                hashedPassword,
                "ADMPS1234A",
                "9876500000",
                "Operations & Compliance"
        );

        // 2. Active Customer with Approved KYC and Initial Funds
        Customer rahul = new Customer(
                "USR-1001",
                "Rahul Sharma",
                "rahul@example.com",
                hashedPassword,
                "ABCDE1234F",
                "9876543210",
                "HDFC-892104"
        );
        userService.save(rahul);

        Account rahulAccount = new Account(
                "ACC-5001",
                "USR-1001",
                BigDecimal.ZERO,
                "HDFC-892104",
                KycStatus.APPROVED
        );
        accountService.save(rahulAccount);
        accountService.deposit(new DepositRequest("USR-1001", new BigDecimal("50000.00"), "HDFC-892104", "Initial Demo Balance"));
        accountService.addToWatchlist("USR-1001", "TCS");
        accountService.addToWatchlist("USR-1001", "INFY");
        accountService.addToWatchlist("USR-1001", "RELIANCE");

        // 3. Customer with Pending KYC
        Customer priya = new Customer(
                "USR-1002",
                "Priya Patel",
                "priya@example.com",
                hashedPassword,
                "PQRST5678G",
                "9876543211",
                "ICICI-451290"
        );
        userService.save(priya);

        Account priyaAccount = new Account(
                "ACC-5002",
                "USR-1002",
                BigDecimal.ZERO,
                "ICICI-451290",
                KycStatus.PENDING
        );
        accountService.save(priyaAccount);
        accountService.deposit(new DepositRequest("USR-1002", new BigDecimal("25000.00"), "ICICI-451290", "Initial Demo Balance"));
        accountService.addToWatchlist("USR-1002", "HDFCBANK");

        // 4. Blocked Account Customer (to test admin blocking rules)
        Customer amit = new Customer(
                "USR-1003",
                "Amit Verma",
                "amit@example.com",
                hashedPassword,
                "XYZAB9012H",
                "9876543212",
                "SBI-783412"
        );
        userService.save(amit);

        Account amitAccount = new Account(
                "ACC-5003",
                "USR-1003",
                BigDecimal.ZERO,
                "SBI-783412",
                KycStatus.APPROVED
        );
        amitAccount.setStatus(AccountStatus.BLOCKED);
        accountService.save(amitAccount);
        accountService.deposit(new DepositRequest("USR-1003", new BigDecimal("10000.00"), "SBI-783412", "Initial Demo Balance"));

        System.out.println("==========================================================");
        System.out.println("  Stock Trading Platform - In-Memory Demo Users Loaded    ");
        System.out.println("  1. Rahul Sharma (Active & KYC Approved) - rahul@example.com");
        System.out.println("  2. Priya Patel  (KYC Pending)           - priya@example.com");
        System.out.println("  3. Amit Verma   (Account Blocked)       - amit@example.com");
        System.out.println("  4. Admin        (System Admin)          - admin@stocktrade.com");
        System.out.println("  Default Password: Password@123");
        System.out.println("==========================================================");
    }
}
