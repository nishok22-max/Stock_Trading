/**
 * Centralized API client for Stock Trading Application.
 * Member 5 - Dashboard & Integration.
 */

export const API_BASE_URL = (import.meta.env.PUBLIC_API_BASE_URL || 'http://localhost:8080').replace(/\/$/, '');

export class ApiError extends Error {
  constructor(message, status = 0, errorCode = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.errorCode = errorCode;
  }
}

/**
 * Common fetch wrapper with error handling and JSON parsing.
 */
async function request(endpoint, options = {}, userId = null) {
  const url = `${API_BASE_URL}${endpoint}`;
  const headers = {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    ...(options.headers || {}),
  };

  if (userId) {
    headers['X-User-Id'] = userId;
  }

  let response;
  try {
    response = await fetch(url, {
      ...options,
      headers,
    });
  } catch (err) {
    throw new ApiError(
      'Network error: unable to reach the backend service at ' + API_BASE_URL + '. Check that the server is running.',
      0,
      'NETWORK_ERROR'
    );
  }

  // Parse response
  const text = await response.text();
  let data = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch (e) {
      data = text;
    }
  }

  if (!response.ok) {
    const message = (data && typeof data === 'object' && (data.message || data.error)) || `HTTP ${response.status}: Request failed`;
    const code = (data && typeof data === 'object' && (data.code || data.errorCode)) || `HTTP_${response.status}`;
    throw new ApiError(message, response.status, code);
  }

  return data;
}

export function apiGet(endpoint, userId = null) {
  return request(endpoint, { method: 'GET' }, userId);
}

export function apiPost(endpoint, body = {}, userId = null, extraHeaders = {}) {
  return request(
    endpoint,
    {
      method: 'POST',
      body: JSON.stringify(body),
      headers: extraHeaders,
    },
    userId
  );
}

export function apiPut(endpoint, body = {}, userId = null) {
  return request(
    endpoint,
    {
      method: 'PUT',
      body: JSON.stringify(body),
    },
    userId
  );
}

export function apiDelete(endpoint, userId = null) {
  return request(endpoint, { method: 'DELETE' }, userId);
}

// ==========================================
// Dashboard API (Member 5)
// ==========================================

export function getDashboardSummary(userId) {
  return apiGet(`/api/dashboard/${userId}`);
}

export function getRecentOrders(userId, limit = 10) {
  return apiGet(`/api/dashboard/${userId}/recent-orders?limit=${limit}`);
}

export function getMarketSummary() {
  return apiGet('/api/dashboard/market-summary');
}

// ==========================================
// Account & Trading Views (Member 1 & Member 2 Integration)
// ==========================================

export function getAccountView(userId) {
  return apiGet('/api/trading/account', userId);
}

export function getMarketView() {
  return apiGet('/api/trading/market');
}

// ==========================================
// Order Management (Member 3 Integration)
// ==========================================

export function placeOrder(userId, orderData, idempotencyKey = null) {
  const headers = idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {};
  return apiPost('/api/orders', orderData, userId, headers);
}

export function previewOrder(userId, orderData) {
  return apiPost('/api/orders/preview', orderData, userId);
}

export function getOrder(userId, orderId) {
  return apiGet(`/api/orders/${orderId}`, userId);
}

export function getOrderHistory(userId) {
  return apiGet(`/api/orders/user/${userId}`, userId);
}

export function cancelOrder(userId, orderId) {
  return apiPost(`/api/orders/${orderId}/cancel`, {}, userId);
}

export function getTradeHistory(userId) {
  return apiGet(`/api/trades/user/${userId}`, userId);
}

