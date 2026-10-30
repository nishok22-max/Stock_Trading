# Member 1 and Member 2 integration contracts

## Member 1: accounts, eligibility and holdings

`AccountGateway` is the only trading dependency for account/holding mutation. Its methods must run inside the same `TradingUnitOfWork.execute` callback as order and trade mutations. The provided `InMemoryAccountGateway` implements that contract with immutable snapshots.

| Method | Required guarantee |
|---|---|
| `account(userId)` | Existing account, current KYC/block flags, available and blocked cash |
| `holding`, `holdings` | Owned and reserved quantities; unreserved = owned − reserved |
| `reserveCash` | Move available → blocked only if available covers the complete amount |
| `releaseCash` | Move exactly the order's reservation blocked → available |
| `reserveShares` | Increase reservation only if unreserved quantity covers it |
| `releaseShares` | Decrease exactly that order's share reservation |
| `settleBuy` | Consume actual cost/fees from reservation, release remainder, increase quantity and average cost |
| `settleSell` | Consume owned/reserved shares, credit net cash, return realized P/L, delete zero holdings |

All failures throw and abort the transaction. Never call deposits, withdrawals, account blocking or holdings writes outside this coordinator when they affect the same state. Existing account and holding services should share these maps/transaction, or replace this entire coordinator with one transaction-capable owner of accounts, holdings, orders and trades. Do not keep a second cash/holding map that can diverge.

Identity must come from Member 1's authenticated session/security principal. The standalone `X-User-Id` header and frontend demo picker are demonstration adapters, not an authentication mechanism. Enforce the principal at the controller boundary; the order ownership checks can remain.

## Member 2: quotes and sessions

`MarketGateway` provides `quote(symbol)`, `stocks()`, `isOpen(Instant)` and `sessionClose(Instant)`. The provided adapter uses positive two-decimal INR quotes and Asia/Kolkata weekday sessions. A quote/session read used for placement or execution must remain consistent for the duration of the trading transaction. Coordinate price/status changes with the same unit of work.

Price update procedure:

1. Under the shared unit of work, validate symbol, active status as appropriate, positive price and the previous-close daily ±10% band; publish the immutable quote.
2. After that transaction returns, invoke `TradingService.processPending()`. It starts independent atomic transactions for each pending order and uses the latest quote.
3. Keep the scheduler enabled as a fallback for quote updates and expiry. Never call the processor from inside another `execute` callback; nested transactions are deliberately rejected.

The Member 2 read APIs are `/api/stocks`, `/api/stocks/{symbol}`, `/api/stocks/search?q=...`, `/api/stocks/{symbol}/price`, `/api/stocks/{symbol}/history`, and `/api/market/status`. Price history is committed with each successful quote mutation in the shared unit of work. The currently available HTTP price mutation is demo-only; do not treat it as an authenticated Admin route. The checked-in project does not yet contain a shared Admin principal, so stock creation and status/price administration must be wired to Member 1's authorization contract before those HTTP operations are exposed.

The demo quote endpoint illustrates that order. Pending orders process in creation-time order, with order ID as a deterministic tie-breaker. A snapshot of pending IDs may include orders another request has cancelled; each processor rechecks current status under the lock and safely skips terminal orders.

## Failure and concurrency contract

- One trade maximum for one order; all-or-nothing execution, no partial fills.
- Reservation, balance/holding mutations, trade persistence and terminal order transition share one commit.
- Do not catch a settlement failure and then commit partial changes. Only pre-reservation business validation produces a recorded REJECTED intent.
- If accounts move to an independently committed remote service, this interface alone is insufficient. Introduce a durable reservation/settlement ledger with idempotent operations and recovery, or keep all settlement in one transactional service.
- Run one backend instance for the required in-memory implementation. Different JVMs have different locks/maps.

## Member 4/5 consumers

`TradeRepository` exposes immutable executions, fees, net cash movement and sell realized P/L. `AccountGateway.holdings` exposes quantities and weighted average buy prices. Downstream portfolio/statement modules should read these facts rather than applying settlement again. All JVM reads join the coordinator. Do not add an event handler that debits cash or changes holdings a second time.
