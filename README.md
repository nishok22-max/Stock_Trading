# Stock Trading App — Module 01: User & Account Management

**Role:** Member 1 — User & Account Management  
**Implementation:** Backend (Java 21 / Spring Boot 3) + Frontend (Modern Financial Dashboard SPA) + Interactive Console CLI  

---

## 📌 Objective
Implement user registration, secure authentication, account management, and financial transactions (deposits, withdrawals, balance enquiry, watchlist, and transaction auditing) strictly adhering to the business rules specified for **Module 1** in the Low-Level Design (LLD) document.

---

## 🏗️ Architecture & Project Structure

The project strictly follows the requested architecture:

```
user/
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, POST /api/auth/login, POST /api/auth/logout
│   └── AccountController.java       # GET /api/account/{userId}, GET /api/account/{userId}/balance,
│                                    # POST /api/account/deposit, POST /api/account/withdraw,
│                                    # GET /api/account/{userId}/transactions, KYC & Watchlist endpoints
│
├── service/
│   ├── UserService.java             # User entity creation, uniqueness checks, lookups
│   ├── AuthService.java             # BCrypt password hashing, authentication, token management
│   └── AccountService.java         # Account creation, deposit, withdraw, balance enquiry, watchlist
│
├── model/
│   ├── User.java                    # Base User entity (Encapsulation & Abstraction)
│   ├── Customer.java                # Customer entity (Inheritance from User)
│   ├── Admin.java                   # Admin entity (Inheritance from User)
│   ├── Account.java                 # Trading Account (Available/Blocked funds, deposit/withdraw rules)
│   ├── Transaction.java             # Financial audit trail record
│   ├── AccountStatus.java           # ACTIVE, BLOCKED
│   ├── KycStatus.java               # PENDING, APPROVED, REJECTED
│   ├── TransactionType.java         # DEPOSIT, WITHDRAWAL, FUNDS_BLOCKED, FUNDS_RELEASED, TRADE_CHARGES
│   └── TransactionStatus.java       # SUCCESS, FAILED
│
├── repository/
│   ├── UserRepository.java          # Thread-safe in-memory User store (ConcurrentHashMap)
│   ├── AccountRepository.java       # Thread-safe in-memory Account store (ConcurrentHashMap)
│   └── TransactionRepository.java   # Thread-safe in-memory Transaction store with indices
│
└── dto/
    ├── RegisterRequest.java         # Name, email, password, unique PAN, phone, bank account, deposit
    ├── LoginRequest.java            # Email, password
    ├── AccountResponse.java         # Full account details, balances, KYC status, watchlist
    ├── DepositRequest.java          # userId/accountId, amount (Rs. 100 - Rs. 1,00,000), notes
    ├── WithdrawRequest.java         # userId/accountId, amount (must <= available balance)
    ├── BalanceResponse.java         # availableBalance, blockedBalance, totalBalance, canTrade
    ├── LoginResponse.java           # Session token, user metadata, account response
    └── ApiResponse.java             # Unified REST response envelope

frontend/ (src/main/resources/static/)
├── index.html                       # Modern SPA dashboard (Overview, Deposits, Withdrawals, History)
├── css/styles.css                   # Glassmorphic dark financial UI styling
└── js/app.js                        # State management, API integration, live validation & toasts
```

---

## 📋 Implementation Tasks Completed

| # | Task | Description | Status |
|---|------|-------------|--------|
| **1** | **User Model** | Created `User` base class, extended by `Customer` and `Admin`. Fields: `userId`, `name`, `email`, `passwordHash`, `panNumber`, `phone`, `role`. Demonstrated **Encapsulation** and **Inheritance**. | ✅ Completed |
| **2** | **Registration & Login Validation** | BCrypt password hashing, regex validation for PAN (`^[A-Z]{5}[0-9]{4}[A-Z]$`), 10-digit phone number, valid email, duplicate email/PAN rejection, credential verification. | ✅ Completed |
| **3** | **Account Creation** | Automatically creates a trading account upon user registration with `availableBalance` and `blockedBalance`. Sets `kycStatus` to `PENDING` per business rules. | ✅ Completed |
| **4** | **Deposit & Withdrawal** | Enforces: Min deposit **Rs. 100**, Max deposit **Rs. 1,00,000** per transaction. Withdrawals allowed **only from Available funds**; balance can **never become negative**. Withdrawals blocked if account is marked `BLOCKED`. | ✅ Completed |
| **5** | **Balance Enquiry** | Real-time calculation and enquiry of Available funds, Blocked funds, and Total balance (`available + blocked`). | ✅ Completed |
| **6** | **Transaction History** | Complete audit trail of all transactions sorted descending by timestamp and transaction ID. Supports filtering by All, Deposits, and Withdrawals. | ✅ Completed |
| **7** | **Exception Handling** | Custom exceptions (`InsufficientBalanceException`, `InvalidTransactionException`, `AccountBlockedException`, `UserAlreadyExistsException`, `UserNotFoundException`, `AccountNotFoundException`) with centralized `@RestControllerAdvice` returning structured error payloads. | ✅ Completed |
| **8** | **JUnit Tests** | 24 comprehensive unit and MockMvc integration tests covering happy paths, boundary conditions, and business rule edge cases. All 24 tests pass. | ✅ Completed |

---

## 🌐 REST API Endpoints

| Method | Endpoint | Purpose | Request Body | Response |
|--------|----------|---------|--------------|----------|
| `POST` | `/api/auth/register` | Register new user & open trading account | `RegisterRequest` | `201 Created` (`AccountResponse`) |
| `POST` | `/api/auth/login` | Authenticate user credentials & issue token | `LoginRequest` | `200 OK` (`LoginResponse`) |
| `POST` | `/api/auth/logout` | Invalidate session token | Bearer Token Header | `200 OK` (`ApiResponse`) |
| `GET` | `/api/account/{userId}` | Get trading account details | None | `200 OK` (`AccountResponse`) |
| `GET` | `/api/account/{userId}/balance` | Check available, blocked & total balance | None | `200 OK` (`BalanceResponse`) |
| `POST` | `/api/account/deposit` | Add funds from linked bank account | `DepositRequest` | `200 OK` (`AccountResponse`) |
| `POST` | `/api/account/withdraw` | Withdraw available funds to bank | `WithdrawRequest` | `200 OK` (`AccountResponse`) |
| `GET` | `/api/account/{userId}/transactions` | View transaction history | None | `200 OK` (`List<Transaction>`) |
| `POST` | `/api/account/{userId}/kyc/approve` | Admin action: Approve KYC for trading | None | `200 OK` (`AccountResponse`) |
| `POST` | `/api/account/{userId}/toggle-block` | Admin action: Block / Unblock account | None | `200 OK` (`AccountResponse`) |
| `GET` | `/api/account/{userId}/watchlist` | View watchlist (max 20 stocks) | None | `200 OK` (`List<String>`) |
| `POST` | `/api/account/{userId}/watchlist/add` | Add stock symbol to watchlist | `{"symbol": "TCS"}` | `200 OK` (`List<String>`) |
| `POST` | `/api/account/{userId}/watchlist/remove` | Remove stock symbol from watchlist | `{"symbol": "TCS"}` | `200 OK` (`List<String>`) |

---

## 💻 Frontend Deliverables

Accessible directly at `http://localhost:8080/` when the server is running:

1. **Login Page**:
   - Clean authentication interface with quick 1-click test pills for pre-seeded users.
2. **Registration Page**:
   - Input fields with client and server-side validation for Name, Email, Password, Indian PAN format, Phone number, Bank account, and optional initial deposit.
3. **Account Overview Dashboard**:
   - Hero balance cards showing Available Funds, Blocked Funds, and Total Balance.
   - Status badges: Account Status (`ACTIVE` / `BLOCKED`), KYC Status (`APPROVED` / `PENDING`), Trading Eligibility (`Enabled` / `Locked`).
   - Profile summary table with User ID, Account ID, PAN, Email, Phone, and Linked Bank.
   - Stock Watchlist widget enforcing maximum 20 stocks.
   - Admin/Tester simulation panel: Toggle KYC Approval and Toggle Account Block directly from the browser!
4. **Deposit & Withdrawal Forms**:
   - Dedicated deposit form with quick amount chips (`+₹500`, `+₹1,000`, `+₹5,000`, `+₹25,000`, `+₹1,00,000`).
   - Dedicated withdrawal form with percentage presets (`25%`, `50%`, `75%`, `100%`) preventing negative balances.
5. **Transaction History Page**:
   - Real-time audit log with transaction ID, green deposit/red withdrawal badges, amounts, resulting balances, statuses, and formatted timestamps. Filterable by All, Deposits, and Withdrawals.

---

## ⚡ How to Run

### Option 1: Run Web Application & Frontend (Recommended)
```bash
# Start Spring Boot application on port 8080
mvn spring-boot:run
```
Then open your browser at:
👉 **`http://localhost:8080/`**

#### Pre-seeded Demo Accounts for Instant Evaluation:
- **Active & KYC Approved:** `rahul@example.com` / `Password@123` (Initial balance: ₹50,000)
- **KYC Pending User:** `priya@example.com` / `Password@123` (Initial balance: ₹25,000)
- **Blocked Account User:** `amit@example.com` / `Password@123` (Initial balance: ₹10,000)
- **System Admin:** `admin@stocktrade.com` / `Password@123`

---

### Option 2: Run Interactive Console Application (CLI)
Per instructions *"Build a console application. You do not need to connect to any database."*:
```bash
mvn compile
mvn exec:java -Dexec.mainClass="com.stocktrading.cli.ConsoleApp"
```

---

### Option 3: Run JUnit Tests
```bash
mvn test
```
**Results:** `Tests run: 24, Failures: 0, Errors: 0, Skipped: 0` (100% Pass)

