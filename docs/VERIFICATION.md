# Verification record

Verified on **30 October 2026**, using Windows, Microsoft OpenJDK **21.0.13**, Maven **3.9.11**, Node **24.19.0**, Astro **7.4.4** and installed Google Chrome through Playwright **1.64.1**.

| Check | Result |
|---|---|
| `scripts/backend.ps1 -Task package` → Maven `verify` | **PASS**; executable Spring Boot JAR produced |
| Brokerage JUnit cases | **7 passed**, zero failures/errors/skips |
| REST/Swagger JUnit cases | **20 passed**, zero failures/errors/skips |
| Trading engine JUnit cases | **53 passed**, zero failures/errors/skips |
| Total JUnit | **80 passed** |
| `npm run build` | **PASS**; Astro strict checks clean and all five routes built |
| `FRONTEND_URL=http://127.0.0.1:4322 npm run test:e2e` | **8 passed** against the built static site and packaged backend JAR |
| `npx --yes newman run docs/Member3.postman_collection.json` | **16 requests, 10 assertions passed** |
| Desktop JavaScript error check | No uncaught page errors |
| Mobile document width at 390 px | No page overflow; holdings table scrolls within its container |

## What the backend checks establish

- Market BUY/SELL settlement, per-order trade linkage, weighted-average purchase price, sell profit/loss and removal of empty holdings.
- Inclusive buy/sell limit matching, pending reservations, execution at improved prices and release of excess buy cash.
- Funds including fees, exact funds, selling with zero starting cash, unowned/reserved shares, pending capacity and release before the next session.
- KYC, blocked buys with permitted sells, suspended stocks, account revalidation, unknown stock/account handling and own-user access boundaries.
- IST opening/closing times, weekend closure, expiry exactly at close before price matching, expiry after a missed session, cancellation and terminal-state guards.
- Malformed JSON, missing fields, invalid enums/UUIDs, fractional quantities, incompatible order/price combinations, disabled demo routes and Swagger paths.
- Rollback after a failure injected after trade persistence, and a pending settlement failure retaining its original reservation for retry.
- Concurrent cash spending, share reservation, 25-order capacity, placement retry keys, and competing execution/cancellation requests.
- A deterministic 200-step mixed-lifecycle test reconciles total cash with all trade cash flows, inventory with all buys/sells, blocked cash with pending buy reservations, reserved shares with pending sells, and executed orders with unique trades after every step.

## Browser journeys

1. Market buy → authoritative review → confirmation → executed history.
2. Sell from a holding → quantity reduction.
3. Pending limit → reserved funds → cancellation → exact release.
4. Quote update crosses a limit → execution at the new price → exactly one trade.
5. Blocked buy, unapproved KYC and overselling show inline errors.
6. The server successfully commits an order, its response is deliberately lost, and a retry with the same key returns one execution.
7. Mobile layout, keyboard submission, native dialog focus/escape behavior and document width.
8. Trade ledger, empty stock search and a disconnected backend state.

The built site was served with `astro preview` and its `/api` proxy. The packaged backend was started with the explicit extended demo session. Normal session timing is tested independently with a controllable clock; tests do not depend on the wall clock or require changing global system time.

## Review artifacts

- `docs/screenshots/trading-desk.png`: desktop trading desk.
- `docs/screenshots/mobile-desk.png`: responsive single-column layout.
- Runtime reports are available locally under `backend/target/surefire-reports`, `frontend/playwright-report`, and `frontend/test-results`; generated outputs are intentionally ignored by Git.

## Remaining team integration

Members 1 and 2 were absent from the initial folder. The module runs with complete in-memory adapters and documented gateway contracts, but the final combined app must wire in their account/market implementations and authenticated principal while preserving the shared transaction boundary. The demo identity header and price-update endpoints are explicitly demonstration facilities. Persistence, multi-instance operation, real exchange connectivity, real-money trading and exchange holidays are outside the requested in-memory simulation.
