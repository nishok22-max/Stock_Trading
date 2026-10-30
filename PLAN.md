# Member 3 implementation plan

The folder initially contained only the three-page problem statement. This implementation follows the requested Java 21 / Spring Boot / Maven / Astro / REST stack, which supersedes the PDF's console-only delivery instruction. Storage remains in memory.

## 1. Domain and dependencies

- Immutable orders, trades, accounts, holdings and stock quotes; BigDecimal for money.
- AccountGateway and MarketGateway contracts stand in for Members 1 and 2.
- Demo adapters supply approved, blocked and unapproved users and simulated stocks.
- A transaction coordinator serializes access and stages immutable state changes. Commit occurs only after the entire operation succeeds; failures discard staged changes.

## 2. Trading engine

- Validate identity, KYC, account restrictions, stock status, market hours, quantity, limit price and pending capacity.
- Reserve buy cash including fees, or sell shares; execute crossing limits immediately at the current quote.
- Atomically settle cash and holdings, publish one trade per order, release excess buy reservation and calculate sell realized P/L.
- Serialize placement, execution, quote changes, cancellation and expiry; optional idempotency keys protect placement retries.
- Poll pending orders, expire at the placement session's close, and release all reservations.

## 3. Frontend and REST

- Astro trade ticket, buy/sell switching, market/limit inputs, server-priced preview, review and confirmation.
- Pending and executed order pages, complete history, trade history and account/holdings visibility.
- Loading, empty, failure, stale preview and cancellation states; responsive keyboard-accessible layout.
- Swagger/OpenAPI, structured errors, Postman collection and explicit demo identity.

## 4. Verification and handoff

- JUnit business-rule, settlement, rollback, REST and concurrency tests.
- Astro typecheck and production build; browser tests for buy, sell, limit, cancel, errors and mobile layout.
- Document exact setup, integration contracts, rounding, session boundaries and intentional scope.

## Acceptance checks

No negative available/blocked balances, no overselling, no duplicate trades, reservation totals match pending orders, expiry is deterministic, and failed settlement never leaves a partial account/holding/order update.

## Completed implementation

All four stages are implemented. Backend verification passes 80 JUnit cases on Java 21, including a 200-step deterministic mixed-lifecycle invariant check and competing placement/execution/cancellation requests. Eight browser journeys pass, including mobile layout and retry after a committed execution loses its response. The Astro typecheck and production build pass. Exact verification commands and final review notes are recorded in `docs/VERIFICATION.md`.
