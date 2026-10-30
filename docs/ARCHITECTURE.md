# Trading architecture

```mermaid
flowchart LR
    UI[Astro trading desk] -->|REST JSON| C[Order / Trade controllers]
    C --> T[TradingService / OrderService]
    T --> U[TradingUnitOfWork: lock + staged immutable state]
    T --> E[OrderExecutionService]
    E --> A[AccountGateway: Member 1]
    E --> R[Order / Trade repositories]
    T --> M[MarketGateway: Member 2]
    E --> B[BrokerageService]
    S[Pending scheduler] --> T
    A --> U
    R --> U
    M --> U
```

```mermaid
sequenceDiagram
    participant User
    participant API
    participant Work as Unit of work
    participant Engine
    participant Account
    participant Ledger as Order / Trade repositories
    User->>API: POST order + user identity + retry key
    API->>Work: Acquire lock; copy maps
    Work->>Engine: Check retry key, expiry, eligibility and quote
    Engine->>Account: Reserve cash + fees OR shares
    Engine->>Ledger: Save pending intent
    alt Quote matches
      Engine->>Account: Settle cash and holdings
      Engine->>Ledger: Save one trade and executed order
    end
    Engine->>Work: Return final immutable response
    alt Successful callback
      Work->>Work: Replace committed state; release lock
      Work-->>API: Order response
      API-->>User: 201 PENDING / EXECUTED / REJECTED
    else Settlement exception
      Work->>Work: Discard staged maps; release lock
      API-->>User: 500; retry with same key
    end
```

The diagram below describes logical entities; **no SQL database is used**. SQL exercises and unrelated modules from the PDF are outside Member 3 scope.

```mermaid
erDiagram
    ACCOUNT ||--o{ ORDER : places
    ACCOUNT ||--o{ HOLDING : owns
    STOCK ||--o{ HOLDING : identifies
    STOCK ||--o{ ORDER : targets
    ORDER ||--o| TRADE : executes_once
    ACCOUNT ||--o{ REQUEST_KEY : scopes
    ORDER ||--o{ REQUEST_KEY : identifies
    ACCOUNT {
      UUID userId PK
      boolean kycApproved
      boolean blocked
      decimal availableBalance
      decimal blockedBalance
    }
    HOLDING {
      UUID userId PK
      string symbol PK
      int quantity
      int reservedQuantity
      decimal averageBuyPrice
    }
    STOCK {
      string symbol PK
      decimal price
      decimal previousClose
      boolean active
    }
    ORDER {
      UUID id PK
      UUID userId FK
      string symbol FK
      string side
      string type
      int quantity
      decimal limitPrice
      string status
      decimal reservedCash
      int reservedShares
      instant expiresAt
    }
    TRADE {
      UUID id PK
      UUID orderId FK
      decimal price
      int quantity
      decimal brokerage
      decimal gst
      decimal realizedPnl
      instant executedAt
    }
    REQUEST_KEY {
      UUID userId PK
      string key PK
      string fingerprint
      UUID orderId FK
    }
```

Encapsulation comes from immutable records and gateway/service methods. `ExecutionRule` is an abstract pricing rule with inherited market and limit implementations; the engine dispatches through its common type. Gateways hide account/market storage details. Controllers depend on services rather than collection manipulation.
