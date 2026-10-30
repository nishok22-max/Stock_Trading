<<<<<<< HEAD
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

=======
# Ledger — Member 3: Order Management & Trading Engine

Standalone backend and frontend implementation, developed from the supplied PDF and Member 3 brief. All five requested endpoints and six frontend deliverables are implemented. The project uses Java 21, Spring Boot, Maven, Astro, REST/JSON, in-memory collections, BigDecimal and JUnit 5.

The PDF requests a console application; the explicit team brief supersedes that delivery format. This module provides REST and Astro screens. Account, stock and holding adapters are included so Member 3 can run before Members 1 and 2 are integrated. Other members' registration, deposits, withdrawals, watchlists, alerts and portfolio modules are outside this delivery.

## Start locally

Requirements: **JDK 21**, **Maven 3.6.3+**, and **Node 22.23.4+ or 24+**. The current workspace also has a local JDK/Maven under `.tools`; the PowerShell helper discovers them automatically. Build tools are ignored by Git.

From the project root, in one terminal:

```powershell
.\scripts\backend.ps1
```

Normal sessions are Monday–Friday, **09:15 inclusive to 15:30 exclusive, Asia/Kolkata**. To demonstrate after hours, explicitly enable the extended simulated session:

```powershell
.\scripts\backend.ps1 -DemoOpen
```

Or use installed tools directly:

```powershell
cd backend
mvn spring-boot:run
```

In a second terminal:

```powershell
cd frontend
npm ci
npm run dev
```

Open [Trading desk](http://localhost:4321), [Swagger UI](http://localhost:8080/swagger-ui.html), or [OpenAPI JSON](http://localhost:8080/v3/api-docs). Astro proxies `/api` to port 8080; `API_PROXY_TARGET` changes it. Production static hosting must proxy `/api` to the backend too. Alternatively set `PUBLIC_API_BASE_URL` and configure a narrow allowed-origin CORS policy on the integrated API. The standalone backend intentionally exposes no permissive CORS policy.

Frontend routes: `/`, `/orders/pending`, `/orders/executed`, `/orders`, `/trades`.

## Demo data and identity

| Demo account | User ID | State |
|---|---|---|
| Aarav Mehta | `11111111-1111-4111-8111-111111111111` | Approved KYC, ₹150,000 available, 12 TCS + 25 INFY |
| Mira Desai | `22222222-2222-4222-8222-222222222222` | Buys blocked; sells permitted, 12 TCS + 25 INFY |
| Rohan Iyer | `33333333-3333-4333-8333-333333333333` | KYC pending; trading rejected |

Prices are dummy values; there is no exchange connection. Demo stock `HALTED` is suspended. Demo data resets when the JVM restarts. `TRADING_DEMO_ENABLED=false` disables demo seeding and the demo price/user endpoints. Supply real team adapters/data before using that setting.

**Integration boundary:** `X-User-Id` represents an identity supplied by a trusted caller in this standalone demo. It is not authentication: anyone who can reach this demo can impersonate a demo user. Member 1 must replace it with an authenticated principal and remove the demo account switch and price endpoints before exposing the integrated application. Within that supplied identity, details, cancellation and histories enforce ownership; request JSON cannot choose another user.

## API

Every private endpoint requires `X-User-Id: <UUID>`. No `userId` belongs in the placement JSON. A whole quantity must be 1–1,000. Prices accept at most two decimals.

| Method | Endpoint | Behavior |
|---|---|---|
| POST | `/api/orders` | Place BUY/SELL MARKET/LIMIT; optional `Idempotency-Key` |
| GET | `/api/orders/{orderId}` | Own order details |
| GET | `/api/orders/user/{userId}` | Own complete order history, newest first |
| POST | `/api/orders/{orderId}/cancel` | Cancel only PENDING; release reservations |
| GET | `/api/trades/user/{userId}` | Own immutable execution ledger |
| POST | `/api/orders/preview` | Server validation and estimated reservation/charges |
| GET | `/api/trading/account` | Account and holdings, including reservations |
| GET | `/api/trading/market` | Stocks, session status, server time, demo override flag |
| GET | `/api/demo/users` | Demo account picker; only when demo enabled |
| PUT | `/api/demo/stocks/{symbol}/price` | Demo price update, ±10% previous-close band; processes pending orders |

Market order:

```json
{"symbol":"INFY","side":"BUY","type":"MARKET","quantity":2}
```

Pending buy limit at the initial quote:

```json
{"symbol":"INFY","side":"BUY","type":"LIMIT","quantity":2,"limitPrice":1500.00}
```

Then PUT `{"price":1490.00}` to `/api/demo/stocks/INFY/price` to cross it during an open session. SELL limits use the inverse price comparison. Crossing limits execute at the **current quote**, giving any price improvement to the user. There are no partial fills.

**HTTP semantics:** valid placement intents return **201** with `PENDING`, `EXECUTED` or `REJECTED`. Business rejections remain in order history with a machine-readable reason prefix. Malformed requests return 400 and create no intent. Preview business errors return 422. Missing accounts/orders return 404; foreign history is 403; terminal cancellation or reused keys with different payloads return 409. Unexpected failures return 500 and discard all staged changes.

Retries using the same user and `Idempotency-Key` return the same order (including its current terminal state) without another reservation/execution. A different body with the same key returns 409. Keys are 1–128 letters, digits, dots, underscores, colons or hyphens. The frontend retains an unresolved key in session storage across network errors and reloads. Server keys live for the JVM lifetime.

## Business behavior

- KYC approval is required for both sides. Blocked accounts can sell but cannot buy. Eligibility is checked at placement and again before pending execution.
- Stock must exist, have a positive quote and be active. Suspended pending orders become REJECTED and release their reservations; holdings remain visible.
- Buy reserves `quantity × reservationPrice + brokerage + GST`. A market order reserves at the current quote; a limit buy reserves at its limit, including an immediately crossing limit. Execution consumes only actual cost and releases the surplus.
- Sell reserves shares, preventing simultaneous pending sells from using them twice. Sale proceeds less charges are credited to available cash in one settlement, so a user can sell with zero cash.
- Brokerage = min(trade value × 0.0005, ₹20), rounded to paise HALF_UP. GST = 18% of that rounded brokerage, also rounded HALF_UP. Maximum total charges are ₹23.60.
- Weighted average buy price excludes charges and is stored at eight decimal places. Sell leaves the remaining average unchanged and deletes zero holdings. Realized P/L is `(executionPrice − averageBuyPrice) × quantity`, rounded to paise, and excludes charges.
- A user can have at most 25 **pending** orders. Immediately executable orders do not consume a pending slot. Expired orders are released before a new placement checks capacity/funds.
- A day order expires at its original session close. Expiry is checked **before** price matching, including after a missed scheduler run or application pause. The scheduler runs once per second; cancellation also recognizes expiry.
- The explicit demo-open override reports itself in the API/UI and assigns a rolling six-hour expiry. Default behavior always uses actual weekday market hours. Holidays are not defined by the problem statement.

## Atomicity and integration

See [integration contracts](docs/INTEGRATION.md) for Members 1 and 2 and [architecture](docs/ARCHITECTURE.md) for sequence and entity diagrams. The [implementation plan](PLAN.md) records the stages and acceptance checks.

The `TradingUnitOfWork` owns a fair lock and copies all maps for each transaction. Records are immutable. Repositories and gateways access only the staged transaction. A successful callback replaces committed state; an exception discards every staged change. Placement, idempotency lookup, capacity checks, reservation, execution, trade creation, cancellation, expiry and quote changes are serialized through it. The repository also rejects a second trade for an order. No `@Transactional` annotation is used to pretend that plain HashMaps support database rollback.

This is a **single-process in-memory** solution, as required. State and retry keys are lost on restart; collection copying and trade uniqueness checks prioritize clarity over large-scale throughput. A remote account service with independent commits cannot preserve these guarantees by merely implementing the interface; use the shared unit of work or redesign around a durable transactional ledger and idempotent reservation protocol.

## Verify

```powershell
.\scripts\backend.ps1 -Task test
.\scripts\backend.ps1 -Task package
cd frontend
npm run build
```

Browser tests require the backend running with `-DemoOpen`, Astro running on port 4321 and installed Google Chrome:

```powershell
cd frontend
npm run test:e2e
```

The tests alter only the simulated in-memory account. Run against a fresh backend for a repeatable starting state. `BACKEND_URL` and `FRONTEND_URL` can override test origins. To use another Playwright browser, change `channel` in `playwright.config.ts` and install its runtime.

JUnit covers pricing/rounding, inclusive limit boundaries, settlement, average cost, realized loss/profit, funds/shares, capacity, KYC, account blocking, suspension, session boundaries, expiry, retry keys, ownership, REST validation, rollback after injected failures, and concurrent overspending/overselling/cancellation/execution/retries. Playwright covers the complete user journeys and mobile layout. See [verification results](docs/VERIFICATION.md) for the recorded run.

Import [the Postman collection](docs/Member3.postman_collection.json) to exercise the API. It was also verified with Newman: 16 requests and 10 assertions passed. Source repository: [nishok22-max/Stock_Trading](https://github.com/nishok22-max/Stock_Trading). The `main` branch contains the initial team project README and Git settings; Member 3 work is developed from that baseline on `feature/member-3-trading-engine`.
>>>>>>> origin/main
