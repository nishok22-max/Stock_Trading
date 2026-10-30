# Module 02: Stock and Market Management

## Scope

Module 02 extends the existing Member 3 application rather than creating a second stock catalog or frontend. The existing immutable `StockQuote` is the shared stock record, `TradingUnitOfWork` owns stock quotes and historical price points, and `MarketGateway` remains the contract used by order management. Quotes are simulated; no external market feed or database is used.

## Backend responsibilities

- `MarketRepository` reads and writes stocks and chronologically sorted price records inside the trading unit of work.
- `StockService` supports listing, case-insensitive partial search, details, current price, and history.
- `MarketDataService` validates stock creation, price updates, daily price bands, and status changes. Its mutation methods are service-level only until shared Admin authorization is available.
- `MarketService` derives status and next opening from the existing Asia/Kolkata market gateway and injected `Clock`.
- `StockController` and `MarketController` expose read-only REST APIs.

Supported sectors for service-level stock creation: `IT`, `BANKING`, `PHARMA`, `ENERGY`, `AUTO`. Status values in API responses are `ACTIVE` and `SUSPENDED`. Suspended stocks remain visible, including existing holdings, while order execution continues to reject suspended symbols through the existing gateway.

Price update validation requires a positive price with at most two decimal places and a value within 10% of the previous close. A successful update changes the current quote and appends a record containing symbol, new price, previous price, and timestamp in the same transaction. The existing demo update route calls `TradingService.processPending()` only after that transaction commits.

## REST APIs

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/stocks` | List stocks by symbol |
| GET | `/api/stocks/{symbol}` | Details, previous close, change, percentage, and status |
| GET | `/api/stocks/search?q=Tata` | Case-insensitive partial match on name or symbol |
| GET | `/api/stocks/{symbol}/price` | Current quote and movement fields |
| GET | `/api/stocks/{symbol}/history` | Chronological history; returns `[]` when the stock has no recorded updates |
| GET | `/api/market/status` | Open/closed state, server time, session close, and next opening time when closed |

Example stock response:

```json
{
  "symbol": "TCS",
  "companyName": "Tata Consultancy Services",
  "sector": "IT",
  "currentPrice": 3542.50,
  "previousClose": 3510.00,
  "priceChange": 32.50,
  "percentageChange": 0.93,
  "status": "ACTIVE"
}
```

Price history records use `symbol`, `price`, `previousPrice`, and ISO-8601 `timestamp`. Market status is computed using weekdays, 09:15 inclusive to 15:30 exclusive, Asia/Kolkata. `nextOpeningTime` is `null` while open.

Invalid or unknown symbols use the shared structured API error response. Missing search query is HTTP 400, unknown stock is 404, and invalid business data uses the existing 422 error shape.

## Admin integration boundary

The checked-out backend contains no Admin user model, Spring Security setup, or authorization principal. `X-User-Id` and the frontend account selector are explicitly documented demo identity and must not be treated as Admin authentication. Therefore this module does not publish stock-create, price-update, or status-update Admin HTTP routes. The service methods are ready to be called from authenticated Admin controllers once Member 1 supplies the shared principal/authorization contract. Do not expose them via an untrusted header.

The existing `PUT /api/demo/stocks/{symbol}/price` remains demo-only, controlled by `trading.demo-enabled`, and must be removed or replaced when integrated with authenticated market administration. It is not an Admin API.

## Astro UI

- The trading desk consumes `/api/stocks` and `/api/market/status` for stock data and the market indicator.
- Stock search matches company name, symbol, and sector.
- Selecting a stock updates its details and Chart.js history chart from `/api/stocks/{symbol}/history`.
- The full detail view is `/stocks?symbol=TCS` (replace `TCS` with the selected symbol).
- Empty history is shown as an empty state; no fabricated chart points are used.

## Run and verify

From the repository root, start the backend with `scripts/backend.ps1`. Start the existing Astro frontend from `frontend` with `npm run dev`. The frontend development proxy forwards `/api` to port 8080.

Backend tests: `scripts/backend.ps1 -Task test`.
Frontend production check: `npm run build`.

Import `docs/Member3.postman_collection.json` and use the “Stock and market management” requests for the read APIs. For demo price changes, run the backend with demo data enabled; each successful update appends history and then processes pending orders.
