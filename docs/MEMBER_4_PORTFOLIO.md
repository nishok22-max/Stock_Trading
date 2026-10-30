# Stock Trading Application – Member 4: Portfolio & Watchlist Management Module

A production-grade, modular, and beginner-friendly **Portfolio and Watchlist Management** subsystem built for a five-member collaborative stock trading platform. Designed to demonstrate clean Java OOP, modern Spring Boot 3 RESTful APIs, BigDecimal financial precision, and an Astro.js fintech dashboard.

---

## 1. Project Overview

In a multi-member stock trading ecosystem, Member 4 provides the single source of truth for:
- Consolidating individual stock positions per user.
- Updating inventory balances and weighted average purchase prices following executed trades.
- Real-time valuation of invested capital and current market holdings.
- Computing unrealized profit/loss and portfolio return percentages.
- Tracking favourite stock tickers via personalized user watchlists.
- Serving REST APIs and rendering an interactive fintech web dashboard.

---

## 2. Technology Stack

### Backend
- **Java 21**: Modern Java features (Records, Pattern Matching, Type Inference).
- **Spring Boot 3.3.4**: Core application bootstrap, RESTful Web MVC, and Dependency Injection.
- **Jakarta Bean Validation**: Robust incoming request validation (`@Valid`, `@NotNull`, `@NotBlank`).
- **In-Memory Storage**: Thread-safe collections (`ConcurrentHashMap`, `CopyOnWriteArrayList`, `AtomicLong`). *No JPA or SQL databases required*.
- **BigDecimal**: High-precision monetary calculations with explicit `RoundingMode.HALF_UP` to prevent floating-point inaccuracies.
- **JUnit 5 & Mockito**: Full unit test coverage and service mocking.
- **Springdoc OpenAPI (Swagger 3)**: Interactive API documentation (`/swagger-ui.html`).

### Frontend
- **Astro.js**: Fast, modern frontend framework.
- **Chart.js**: Dynamic donut chart visualizing portfolio weight distribution.
- **Vanilla CSS**: Custom fintech design system with dark mode aesthetic, vibrant accents, glassmorphic cards, and responsive sidebar navigation.

---

## 3. Architecture & Separation of Concerns

```
                  ┌─────────────────────────────────────┐
                  │    Astro.js Fintech Dashboard       │
                  │  (Holdings Table, Allocation Chart) │
                  └──────────────────┬──────────────────┘
                                     │ HTTP / JSON
                                     ▼
                  ┌─────────────────────────────────────┐
                  │          REST Controllers           │
                  │ (PortfolioController, WatchlistCtrl)│
                  └──────────────────┬──────────────────┘
                                     │
                                     ▼
                  ┌─────────────────────────────────────┐
                  │            Service Layer            │
                  │  - HoldingService (Weighted Avg)    │
                  │  - PortfolioService (P&L & Returns) │
                  │  - WatchlistService (Deduplication) │
                  └──────┬──────────────────────┬───────┘
                         │                      │
       Integration Boundary                     │
   ┌─────────────────────┴────────────┐         ▼
   │ StockPriceProvider (Member 2)    │   ┌─────────────────────────────┐
   │ ExecutedTrade (Member 5)         │   │     In-Memory Repository    │
   └──────────────────────────────────┘   │  (ConcurrentHashMap Store)  │
                                          └─────────────┬───────────────┘
                                                        │
                                                        ▼
                                          ┌─────────────────────────────┐
                                          │      Domain Entities        │
                                          │  - Holding (HoldingKey)     │
                                          │  - Portfolio                │
                                          │  - WatchlistItem            │
                                          └─────────────────────────────┘
```

---

## 4. Key Financial Calculations & Formulas

### A. Weighted Average Purchase Price (HoldingService)
When buying additional shares of an already owned stock:

$$\text{New Average Price} = \frac{(\text{Old Quantity} \times \text{Old Average Price}) + (\text{New Quantity} \times \text{New Purchase Price})}{\text{Total Quantity}}$$

*Rule on Selling:* When selling shares, the average purchase price of remaining shares remains **unchanged**. If quantity reaches 0, the holding position is completely deleted from memory.

### B. Portfolio Valuation Metrics (PortfolioService)
For each holding $i$:
- **Invested Value**: $\text{Quantity}_i \times \text{Average Purchase Price}_i$
- **Current Value**: $\text{Quantity}_i \times \text{Current Market Price}_i$
- **Unrealized P&L**: $\text{Current Value}_i - \text{Invested Value}_i$
- **Return %**: $(\frac{\text{Unrealized P&L}_i}{\text{Invested Value}_i}) \times 100$
- **Weight Allocation %**: $(\frac{\text{Current Value}_i}{\text{Total Portfolio Current Value}}) \times 100$

Zero investment amounts are safely handled returning `0.00%` without `ArithmeticException`.

---

## 5. Team Integration Boundaries

This module was created to integrate seamlessly into a 5-member platform:
- **Member 1 (User & Funds)**: Supplies the user identity (`userId`).
- **Member 2 (Stock & Market)**: Provides live ticker prices and symbol validation via `StockPriceProvider`.
- **Member 3 (Order Placement)**: Places user buy/sell orders.
- **Member 4 (Portfolio & Watchlist)**: *This module*.
- **Member 5 (Trades & Execution)**: Executes orders and emits `ExecutedTrade` events.

### Decoupled Interfaces:
1. **`StockPriceProvider`**:
   ```java
   public interface StockPriceProvider {
       BigDecimal getCurrentPrice(String stockSymbol);
       boolean isValidStock(String stockSymbol);
       Map<String, BigDecimal> getPrices(Iterable<String> stockSymbols);
   }
   ```
2. **`ExecutedTrade`**:
   ```json
   {
     "tradeId": 1001,
     "userId": 101,
     "stockSymbol": "TCS",
     "side": "BUY",
     "quantity": 10,
     "executionPrice": 3600.00,
     "executedAt": "2026-10-30T10:15:30"
   }
   ```

---

## 6. REST API Endpoints

### Portfolio Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/portfolio/{userId}` | Retrieve user's consolidated portfolio valuation and holding details |
| `GET` | `/api/portfolio/{userId}/holdings` | Retrieve list of user's active holdings with live market metrics |
| `GET` | `/api/portfolio/{userId}/performance` | Retrieve analytical performance metrics (gainers/losers count, best/worst performers) |
| `POST` | `/api/portfolio/trades/execute` | Process an executed trade from Member 5 |

### Watchlist Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/watchlist/{userId}` | Retrieve user's watchlist enriched with live prices |
| `POST` | `/api/watchlist` | Add a stock ticker to user's watchlist |
| `DELETE` | `/api/watchlist/{userId}/{symbol}` | Remove a stock ticker from user's watchlist |

### Sample API Payloads

#### 1. Add Stock to Watchlist
**`POST /api/watchlist`**
```json
{
  "userId": 101,
  "stockSymbol": "TCS"
}
```
**Response (201 Created):**
```json
{
  "id": 1,
  "userId": 101,
  "stockSymbol": "TCS",
  "addedAt": "2026-10-30T14:45:00"
}
```

#### 2. Get User Portfolio
**`GET /api/portfolio/101`**
```json
{
  "userId": 101,
  "totalInvestedValue": 172900.00,
  "totalCurrentValue": 191636.25,
  "totalUnrealizedProfitLoss": 18736.25,
  "totalReturnPercentage": 10.84,
  "totalHoldingsCount": 4,
  "holdings": [
    {
      "stockSymbol": "TCS",
      "quantity": 20,
      "averagePurchasePrice": 3700.00,
      "currentMarketPrice": 3920.50,
      "investedValue": 74000.00,
      "currentValue": 78410.00,
      "unrealizedProfitLoss": 4410.00,
      "returnPercentage": 5.96,
      "allocationPercentage": 40.92
    }
  ]
}
```

---

## 7. Setup & Execution Instructions

### Prerequisites
- Java 21 or higher
- Node.js 18+ and npm

### Running the Backend

```bash
cd backend

# On Windows:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-26.0.1"  # Or your JDK directory
.\mvnw.cmd spring-boot:run

# On Linux / macOS:
./mvnw spring-boot:run
```

- Backend server: `http://localhost:8080`
- Interactive Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI Specification: `http://localhost:8080/v3/api-docs`

### Running the Frontend

```bash
cd frontend
npm install
npm run dev
```

- Frontend dashboard: `http://localhost:4321`

---

## 8. Unit Testing & Verification

Run the entire test suite via Maven:

```bash
cd backend
.\mvnw.cmd test
```

### Test Coverage Summary:
- **`HoldingServiceTest` (8 tests)**:
  - First buy holding creation
  - Second buy weighted average purchase price
  - Partial sell keeping average purchase price intact
  - Complete sell removing holding when quantity reaches 0
  - Rejection of sell orders exceeding owned shares
  - Validation of non-positive trade quantities
  - Validation of zero or negative trade prices
  - Processing `ExecutedTrade` DTOs
- **`PortfolioServiceTest` (5 tests)**:
  - Invested value calculation
  - Current market value calculation
  - Positive unrealized P&L
  - Negative unrealized P&L
  - Safe zero-investment handling
- **`WatchlistServiceTest` (6 tests)**:
  - Adding stocks
  - Duplicate stock rejection
  - Removing stocks
  - Nonexistent stock removal error
  - Multi-user isolation
  - Invalid stock symbol validation
- **`ModelClassesTest` & Controller Tests (10 tests)**:
  - `HoldingKey` composite record equality and uppercase normalization
  - Encapsulation & invariants
  - MockMvc HTTP status code and JSON serialization verification

**Total: 29 tests, 0 failures, 0 errors, 100% pass rate.**

---

## 9. Known In-Memory Storage Limitations

1. **Volatile Lifecycle**: In-memory collections reset when the JVM restarts.
2. **Single Instance**: Data cannot be shared across multiple horizontal server instances without a distributed cache or shared database.
3. **Memory Sizing**: Dataset size is bounded by the JVM heap space allocated to the application.
