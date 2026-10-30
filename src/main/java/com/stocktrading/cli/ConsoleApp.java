package com.stocktrading.cli;

import com.stocktrading.user.dto.AccountResponse;
import com.stocktrading.user.dto.BalanceResponse;
import com.stocktrading.user.dto.DepositRequest;
import com.stocktrading.user.dto.LoginRequest;
import com.stocktrading.user.dto.LoginResponse;
import com.stocktrading.user.dto.RegisterRequest;
import com.stocktrading.user.dto.WithdrawRequest;
import com.stocktrading.user.model.Transaction;
import com.stocktrading.user.repository.AccountRepository;
import com.stocktrading.user.repository.TransactionRepository;
import com.stocktrading.user.repository.UserRepository;
import com.stocktrading.user.service.AccountService;
import com.stocktrading.user.service.AuthService;
import com.stocktrading.user.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Scanner;

/**
 * Standalone interactive console application supporting CLI evaluation
 * per the instructions: "Build a console application. You do not need to connect to any database."
 */
public class ConsoleApp {

    private final UserService userService;
    private final AccountService accountService;
    private final AuthService authService;
    private String currentUserId = null;
    private String currentUserName = null;

    public ConsoleApp() {
        UserRepository userRepository = new UserRepository();
        AccountRepository accountRepository = new AccountRepository();
        TransactionRepository transactionRepository = new TransactionRepository();

        this.userService = new UserService(userRepository);
        this.accountService = new AccountService(accountRepository, transactionRepository, userRepository);
        this.authService = new AuthService(userService, accountService);

        seedDemoData();
    }

    private void seedDemoData() {
        String hashed = authService.getPasswordEncoder().encode("Password@123");
        userService.createCustomer(
                new RegisterRequest("Rahul Sharma", "rahul@example.com", "Password@123", "ABCDE1234F", "9876543210", "HDFC-892104", new BigDecimal("50000.00")),
                hashed
        );
        accountService.createAccount("USR-1101", "HDFC-892104", new BigDecimal("50000.00"));
        accountService.approveKyc("USR-1101");
    }

    public static void main(String[] args) {
        ConsoleApp app = new ConsoleApp();
        app.start();
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("===============================================================");
        System.out.println("      STOCK TRADING APP - MODULE 1 (CONSOLE CLI INTERFACE)     ");
        System.out.println("===============================================================");

        while (true) {
            if (currentUserId == null) {
                printUnauthMenu();
                System.out.print("Select an option (1-3): ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> handleLogin(scanner);
                    case "2" -> handleRegister(scanner);
                    case "3" -> {
                        System.out.println("Exiting Console Application. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option! Please enter 1, 2, or 3.");
                }
            } else {
                printAuthMenu();
                System.out.print("Select an option (1-8): ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> handleAccountOverview();
                    case "2" -> handleBalanceEnquiry();
                    case "3" -> handleDeposit(scanner);
                    case "4" -> handleWithdraw(scanner);
                    case "5" -> handleTransactionHistory();
                    case "6" -> handleWatchlist(scanner);
                    case "7" -> handleLogout();
                    case "8" -> {
                        System.out.println("Exiting Console Application. Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option! Please enter between 1 and 8.");
                }
            }
            System.out.println();
        }
    }

    private void printUnauthMenu() {
        System.out.println("\n--- MAIN MENU ---");
        System.out.println("1. Login (Demo user: rahul@example.com / Password@123)");
        System.out.println("2. Register New User");
        System.out.println("3. Exit");
    }

    private void printAuthMenu() {
        System.out.println("\n--- USER DASHBOARD [" + currentUserName + " (" + currentUserId + ")] ---");
        System.out.println("1. View Account Details");
        System.out.println("2. Check Balance (Enquiry)");
        System.out.println("3. Deposit Funds (Min Rs. 100, Max Rs. 1,00,000)");
        System.out.println("4. Withdraw Funds");
        System.out.println("5. View Transaction History");
        System.out.println("6. Manage Watchlist (Max 20 stocks)");
        System.out.println("7. Logout");
        System.out.println("8. Exit Application");
    }

    private void handleLogin(Scanner scanner) {
        System.out.print("Enter Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        try {
            LoginResponse response = authService.login(new LoginRequest(email, password));
            this.currentUserId = response.getUserId();
            this.currentUserName = response.getName();
            System.out.println("SUCCESS: Welcome back, " + currentUserName + "!");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleRegister(Scanner scanner) {
        System.out.print("Full Name: ");
        String name = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Password: ");
        String password = scanner.nextLine().trim();
        System.out.print("PAN Number (e.g. ABCDE1234F): ");
        String pan = scanner.nextLine().trim().toUpperCase();
        System.out.print("Phone Number (10 digits): ");
        String phone = scanner.nextLine().trim();
        System.out.print("Linked Bank Account: ");
        String bank = scanner.nextLine().trim();
        System.out.print("Initial Deposit Amount (optional, min Rs. 100, press enter to skip): ");
        String depStr = scanner.nextLine().trim();
        BigDecimal initialDeposit = depStr.isEmpty() ? null : new BigDecimal(depStr);

        try {
            RegisterRequest req = new RegisterRequest(name, email, password, pan, phone, bank, initialDeposit);
            AccountResponse response = authService.register(req);
            System.out.println("SUCCESS: Account created successfully!");
            System.out.println("User ID: " + response.getUserId() + " | Account ID: " + response.getAccountId());
            System.out.println("Please sign in with your credentials.");
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleAccountOverview() {
        try {
            AccountResponse acc = accountService.getAccountResponseByUserId(currentUserId);
            System.out.println("\n=== ACCOUNT OVERVIEW ===");
            System.out.println("Account ID        : " + acc.getAccountId());
            System.out.println("User ID           : " + acc.getUserId());
            System.out.println("Name              : " + acc.getUserName());
            System.out.println("Email             : " + acc.getUserEmail());
            System.out.println("PAN Number        : " + acc.getPanNumber());
            System.out.println("Phone             : " + acc.getPhone());
            System.out.println("Linked Bank       : " + acc.getLinkedBankAccount());
            System.out.println("Available Balance : Rs. " + acc.getAvailableBalance());
            System.out.println("Blocked Balance   : Rs. " + acc.getBlockedBalance());
            System.out.println("Total Balance     : Rs. " + acc.getTotalBalance());
            System.out.println("Account Status    : " + acc.getStatus());
            System.out.println("KYC Status        : " + acc.getKycStatus());
            System.out.println("Can Trade?        : " + (acc.isCanTrade() ? "YES" : "NO (KYC Approval Required)"));
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleBalanceEnquiry() {
        try {
            BalanceResponse bal = accountService.getBalanceByUserId(currentUserId);
            System.out.println("\n=== BALANCE ENQUIRY ===");
            System.out.println("Available Funds: Rs. " + bal.getAvailableBalance());
            System.out.println("Blocked Funds  : Rs. " + bal.getBlockedBalance());
            System.out.println("Total Balance  : Rs. " + bal.getTotalBalance());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleDeposit(Scanner scanner) {
        System.out.print("Enter Deposit Amount (Rs. 100 to Rs. 1,00,000): Rs. ");
        String amtStr = scanner.nextLine().trim();
        try {
            BigDecimal amount = new BigDecimal(amtStr);
            AccountResponse acc = accountService.deposit(new DepositRequest(currentUserId, amount, null, "CLI Console Deposit"));
            System.out.println("SUCCESS: Deposited Rs. " + amount);
            System.out.println("New Available Balance: Rs. " + acc.getAvailableBalance());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleWithdraw(Scanner scanner) {
        System.out.print("Enter Withdrawal Amount: Rs. ");
        String amtStr = scanner.nextLine().trim();
        try {
            BigDecimal amount = new BigDecimal(amtStr);
            AccountResponse acc = accountService.withdraw(new WithdrawRequest(currentUserId, amount, null, "CLI Console Withdrawal"));
            System.out.println("SUCCESS: Withdrew Rs. " + amount);
            System.out.println("New Available Balance: Rs. " + acc.getAvailableBalance());
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleTransactionHistory() {
        try {
            List<Transaction> txns = accountService.getTransactionHistory(currentUserId);
            System.out.println("\n=== TRANSACTION HISTORY (" + txns.size() + " records) ===");
            if (txns.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }
            System.out.printf("%-12s | %-12s | %-12s | %-14s | %-10s | %s%n", 
                    "Txn ID", "Type", "Amount", "Balance After", "Status", "Timestamp");
            System.out.println("----------------------------------------------------------------------------------");
            for (Transaction t : txns) {
                System.out.printf("%-12s | %-12s | Rs. %-8s | Rs. %-10s | %-10s | %s%n",
                        t.getTransactionId(),
                        t.getType(),
                        t.getAmount(),
                        t.getBalanceAfter(),
                        t.getStatus(),
                        t.getTimestamp());
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleWatchlist(Scanner scanner) {
        try {
            List<String> watchlist = accountService.getWatchlist(currentUserId);
            System.out.println("\nCurrent Watchlist (" + watchlist.size() + "/20): " + watchlist);
            System.out.println("1. Add Stock");
            System.out.println("2. Remove Stock");
            System.out.println("3. Back");
            System.out.print("Choice: ");
            String opt = scanner.nextLine().trim();
            if ("1".equals(opt)) {
                System.out.print("Enter Stock Symbol (e.g. TCS): ");
                String sym = scanner.nextLine().trim().toUpperCase();
                accountService.addToWatchlist(currentUserId, sym);
                System.out.println("Added " + sym + " to watchlist!");
            } else if ("2".equals(opt)) {
                System.out.print("Enter Stock Symbol to remove: ");
                String sym = scanner.nextLine().trim().toUpperCase();
                accountService.removeFromWatchlist(currentUserId, sym);
                System.out.println("Removed " + sym + " from watchlist!");
            }
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
        }
    }

    private void handleLogout() {
        System.out.println("Logged out successfully.");
        this.currentUserId = null;
        this.currentUserName = null;
    }
}

