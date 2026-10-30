# Stock Trading App — Design & SQL Exercise Solutions

This document provides the solutions to the SQL exercises, Entity-Relationship (ER) diagram, and High-Level System Architecture diagram specified in the Low Level Design (LLD) document.

---

## 🗄️ SQL Exercise Solutions

### Database Schema Context
For the queries below, assume the standard relational schema modeled from the problem statement:
- `users(user_id, name, email, pan_number, phone, role, created_at)`
- `accounts(account_id, user_id, available_balance, blocked_balance, status, kyc_status)`
- `stocks(stock_id, symbol, company_name, sector, current_price, status)`
- `orders(order_id, user_id, stock_id, side, type, price, quantity, status, created_at)`
- `trades(trade_id, order_id, stock_id, buyer_user_id, seller_user_id, price, quantity, executed_at, brokerage, gst)`

---

### Query 1
> **Requirement:** For each sector, find the total number of stocks listed, the total number of executed trades, and the total traded value. Show only sectors that have more than 2 stocks. Sort by total traded value in descending order.

```sql
SELECT 
    s.sector,
    COUNT(DISTINCT s.stock_id) AS total_stocks_listed,
    COUNT(t.trade_id) AS total_executed_trades,
    COALESCE(SUM(t.price * t.quantity), 0.00) AS total_traded_value
FROM 
    stocks s
LEFT JOIN 
    trades t ON s.stock_id = t.stock_id
GROUP BY 
    s.sector
HAVING 
    COUNT(DISTINCT s.stock_id) > 2
ORDER BY 
    total_traded_value DESC;
```

---

### Query 2
> **Requirement:** For each user, list the total number of orders placed, the number of executed orders, and the total brokerage paid. Group the results by user and include only users who have placed at least 3 orders.

```sql
SELECT 
    u.user_id,
    u.name,
    COUNT(o.order_id) AS total_orders_placed,
    COUNT(CASE WHEN o.status = 'EXECUTED' THEN 1 END) AS executed_orders_count,
    COALESCE(SUM(t.brokerage), 0.00) AS total_brokerage_paid
FROM 
    users u
JOIN 
    orders o ON u.user_id = o.user_id
LEFT JOIN 
    trades t ON o.order_id = t.order_id
GROUP BY 
    u.user_id, 
    u.name
HAVING 
    COUNT(o.order_id) >= 3
ORDER BY 
    total_orders_placed DESC;
```

---

### Query 3
> **Requirement:** For each stock, find the total quantity bought, the total quantity sold, and the number of distinct users who traded it. Include only stocks traded by more than 2 distinct users, ordered by total traded quantity in descending order.

```sql
SELECT 
    s.symbol,
    s.company_name,
    COALESCE(SUM(CASE WHEN o.side = 'BUY' THEN t.quantity ELSE 0 END), 0) AS total_quantity_bought,
    COALESCE(SUM(CASE WHEN o.side = 'SELL' THEN t.quantity ELSE 0 END), 0) AS total_quantity_sold,
    COUNT(DISTINCT o.user_id) AS distinct_users_count,
    COALESCE(SUM(t.quantity), 0) AS total_traded_quantity
FROM 
    stocks s
JOIN 
    trades t ON s.stock_id = t.stock_id
JOIN 
    orders o ON t.order_id = o.order_id
GROUP BY 
    s.stock_id, 
    s.symbol, 
    s.company_name
HAVING 
    COUNT(DISTINCT o.user_id) > 2
ORDER BY 
    total_traded_quantity DESC;
```

---

## 📊 Entity Relationship (ER) Diagram

```mermaid
erDiagram
    USERS ||--|| ACCOUNTS : "has"
    USERS ||--o{ WATCHLISTS : "maintains (max 20)"
    USERS ||--o{ ORDERS : "places (max 25 pending)"
    USERS ||--o{ HOLDINGS : "owns"
    USERS ||--o{ PRICE_ALERTS : "sets (max 10 active)"
    ACCOUNTS ||--o{ TRANSACTIONS : "records"
    STOCKS ||--o{ ORDERS : "referenced in"
    STOCKS ||--o{ TRADES : "executed in"
    STOCKS ||--o{ HOLDINGS : "held in"
    STOCKS ||--o{ WATCHLISTS : "included in"
    STOCKS ||--o{ PRICE_ALERTS : "tracked in"
    ORDERS ||--o{ TRADES : "generates"

    USERS {
        string user_id PK
        string name
        string email UK
        string password_hash
        string pan_number UK
        string phone
        string role
        timestamp created_at
    }

    ACCOUNTS {
        string account_id PK
        string user_id FK
        decimal available_balance
        decimal blocked_balance
        string status
        string kyc_status
        string linked_bank_account
        timestamp updated_at
    }

    TRANSACTIONS {
        string transaction_id PK
        string account_id FK
        string user_id FK
        string type
        decimal amount
        decimal balance_after
        string status
        string description
        timestamp timestamp
    }

    STOCKS {
        string stock_id PK
        string symbol UK
        string company_name
        string sector
        decimal current_price
        string status
    }

    ORDERS {
        string order_id PK
        string user_id FK
        string stock_id FK
        string side
        string type
        decimal price
        int quantity
        string status
        timestamp created_at
    }

    TRADES {
        string trade_id PK
        string order_id FK
        string stock_id FK
        decimal price
        int quantity
        decimal brokerage
        decimal gst
        timestamp executed_at
    }

    HOLDINGS {
        string holding_id PK
        string user_id FK
        string stock_id FK
        int quantity
        decimal avg_buy_price
    }

    WATCHLISTS {
        string watchlist_id PK
        string user_id FK
        string stock_symbol
    }

    PRICE_ALERTS {
        string alert_id PK
        string user_id FK
        string stock_id FK
        decimal target_price
        string status
    }
```

---

## 🏛️ High-Level System Architecture Diagram

```mermaid
flowchart TD
    subgraph ClientLayer ["Client Layer"]
        WEB["Modern Web Frontend (SPA)<br/>HTML5 / CSS3 / Vanilla JS"]
        CLI["Interactive Console CLI<br/>ConsoleApp.java"]
        REST["REST API Clients / Tests<br/>MockMvc / Postman"]
    end

    subgraph Module1 ["Module 1: User & Account Management (Our Role)"]
        AUTH_CTRL["AuthController<br/>/api/auth/register, /api/auth/login"]
        ACC_CTRL["AccountController<br/>/api/account/*"]
        AUTH_SVC["AuthService<br/>BCrypt Password Security"]
        ACC_SVC["AccountService<br/>Funds Engine (Min 100, Max 100k)"]
        USER_REPO[("In-Memory User Repo")]
        ACC_REPO[("In-Memory Account Repo<br/>Available vs Blocked Funds")]
        TXN_REPO[("In-Memory Transaction Repo<br/>Full Audit Trail")]
    end

    subgraph Module2 ["Module 2: Stocks & Market"]
        STOCK_SVC["Stock & Market Service<br/>Prices, Sectors, Market Hours"]
    end

    subgraph Module3 ["Module 3: Order Placement"]
        ORDER_SVC["Order Service<br/>Market/Limit (Max 25 Pending)"]
    end

    subgraph Module4 ["Module 4: Trades, Holdings & Charges"]
        TRADE_SVC["Trade Execution Engine<br/>Brokerage (0.05% cap 20) + 18% GST"]
        HOLDING_SVC["Holdings Service<br/>Weighted Avg Price"]
    end

    subgraph Module5 ["Module 5: Portfolio & Alerts"]
        PORTFOLIO_SVC["Portfolio & P/L Calculator<br/>Realized & Unrealized P/L"]
        ALERT_SVC["Price Alert Engine<br/>Max 10 Active Alerts"]
    end

    %% Client Interactions
    WEB --> AUTH_CTRL
    WEB --> ACC_CTRL
    CLI --> AUTH_SVC
    CLI --> ACC_SVC
    REST --> AUTH_CTRL
    REST --> ACC_CTRL

    AUTH_CTRL --> AUTH_SVC
    ACC_CTRL --> ACC_SVC
    AUTH_SVC --> USER_REPO
    ACC_SVC --> ACC_REPO
    ACC_SVC --> TXN_REPO

    %% Inter-module coordination
    ACC_SVC -- "1. Verify KYC & Active Status" --> ORDER_SVC
    ORDER_SVC -- "2. Block funds for Buy order" --> ACC_SVC
    ORDER_SVC -- "3. Release funds if cancelled" --> ACC_SVC
    TRADE_SVC -- "4. Deduct blocked funds + Brokerage" --> ACC_SVC
    TRADE_SVC --> HOLDING_SVC
    HOLDING_SVC --> PORTFOLIO_SVC
    STOCK_SVC --> ALERT_SVC
```

