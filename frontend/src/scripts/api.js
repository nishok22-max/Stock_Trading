/**
 * API Client helper connecting the frontend to Spring Boot backend on http://localhost:8080/api.
 *
 * Implements fallback mock data cleanly separated from live API responses.
 */

const API_BASE_URL = 'http://localhost:8080/api';
export const DEFAULT_USER_ID = 101;

// Separate Mock Data for Offline/Local Dev Fallback
const FALLBACK_PORTFOLIO = {
  isMockData: true,
  userId: 101,
  totalInvestedValue: 172900.00,
  totalCurrentValue: 191636.25,
  totalUnrealizedProfitLoss: 18736.25,
  totalReturnPercentage: 10.84,
  totalHoldingsCount: 4,
  holdings: [
    {
      stockSymbol: "TCS",
      quantity: 20,
      averagePurchasePrice: 3700.00,
      currentMarketPrice: 3920.50,
      investedValue: 74000.00,
      currentValue: 78410.00,
      unrealizedProfitLoss: 4410.00,
      returnPercentage: 5.96,
      allocationPercentage: 40.92
    },
    {
      stockSymbol: "INFY",
      quantity: 25,
      averagePurchasePrice: 1650.00,
      currentMarketPrice: 1845.00,
      investedValue: 41250.00,
      currentValue: 46125.00,
      unrealizedProfitLoss: 4875.00,
      returnPercentage: 11.82,
      allocationPercentage: 24.07
    },
    {
      stockSymbol: "RELIANCE",
      quantity: 15,
      averagePurchasePrice: 2820.00,
      currentMarketPrice: 2980.75,
      investedValue: 42300.00,
      currentValue: 44711.25,
      unrealizedProfitLoss: 2411.25,
      returnPercentage: 5.70,
      allocationPercentage: 23.33
    },
    {
      stockSymbol: "HDFCBANK",
      quantity: 10,
      averagePurchasePrice: 1535.00,
      currentMarketPrice: 1680.20,
      investedValue: 15350.00,
      currentValue: 16802.00,
      unrealizedProfitLoss: 1452.00,
      returnPercentage: 9.46,
      allocationPercentage: 8.77
    }
  ]
};

const FALLBACK_WATCHLIST = [
  { id: 1, userId: 101, stockSymbol: "AAPL", currentPrice: 230.50, addedAt: new Date().toISOString() },
  { id: 2, userId: 101, stockSymbol: "TATAMOTORS", currentPrice: 965.30, addedAt: new Date().toISOString() },
  { id: 3, userId: 101, stockSymbol: "NVDA", currentPrice: 135.40, addedAt: new Date().toISOString() },
  { id: 4, userId: 101, stockSymbol: "MSFT", currentPrice: 445.00, addedAt: new Date().toISOString() }
];

/**
 * Fetch consolidated portfolio for a user.
 */
export async function getPortfolio(userId = DEFAULT_USER_ID) {
  try {
    const response = await fetch(`${API_BASE_URL}/portfolio/${userId}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    const data = await response.json();
    return { ...data, isMockData: false };
  } catch (err) {
    console.warn(`[API] Failed to fetch live portfolio from backend (${err.message}). Using fallback data.`, err);
    return FALLBACK_PORTFOLIO;
  }
}

/**
 * Fetch individual holdings for a user.
 */
export async function getHoldings(userId = DEFAULT_USER_ID) {
  try {
    const response = await fetch(`${API_BASE_URL}/portfolio/${userId}/holdings`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (err) {
    console.warn(`[API] Failed to fetch live holdings (${err.message}). Using fallback data.`);
    return FALLBACK_PORTFOLIO.holdings;
  }
}

/**
 * Fetch portfolio performance analytics.
 */
export async function getPerformance(userId = DEFAULT_USER_ID) {
  try {
    const response = await fetch(`${API_BASE_URL}/portfolio/${userId}/performance`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (err) {
    console.warn(`[API] Failed to fetch live performance (${err.message}).`);
    return {
      userId,
      totalInvestedValue: FALLBACK_PORTFOLIO.totalInvestedValue,
      totalCurrentValue: FALLBACK_PORTFOLIO.totalCurrentValue,
      totalUnrealizedProfitLoss: FALLBACK_PORTFOLIO.totalUnrealizedProfitLoss,
      totalReturnPercentage: FALLBACK_PORTFOLIO.totalReturnPercentage,
      totalHoldingsCount: 4,
      profitableHoldingsCount: 4,
      lossMakingHoldingsCount: 0,
      bestPerformerSymbol: "INFY",
      bestPerformerReturnPercentage: 11.82,
      worstPerformerSymbol: "TCS",
      worstPerformerReturnPercentage: 5.96
    };
  }
}

/**
 * Fetch user watchlist with live prices.
 */
export async function getWatchlist(userId = DEFAULT_USER_ID) {
  try {
    const response = await fetch(`${API_BASE_URL}/watchlist/${userId}`);
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return await response.json();
  } catch (err) {
    console.warn(`[API] Failed to fetch live watchlist (${err.message}). Using fallback data.`);
    return FALLBACK_WATCHLIST;
  }
}

/**
 * Add stock to watchlist.
 */
export async function addToWatchlist(userId, stockSymbol) {
  const response = await fetch(`${API_BASE_URL}/watchlist`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, stockSymbol })
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ message: 'Failed to add stock' }));
    throw new Error(errorBody.message || `HTTP ${response.status}`);
  }
  return await response.json();
}

/**
 * Remove stock from watchlist.
 */
export async function removeFromWatchlist(userId, stockSymbol) {
  const response = await fetch(`${API_BASE_URL}/watchlist/${userId}/${encodeURIComponent(stockSymbol)}`, {
    method: 'DELETE'
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ message: 'Failed to remove stock' }));
    throw new Error(errorBody.message || `HTTP ${response.status}`);
  }
  return await response.json();
}

/**
 * Simulate trade execution across Member 5 boundary.
 */
export async function executeTrade(tradeData) {
  const response = await fetch(`${API_BASE_URL}/portfolio/trades/execute`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tradeData)
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ message: 'Trade execution failed' }));
    throw new Error(errorBody.message || `HTTP ${response.status}`);
  }
  return await response.json();
}
