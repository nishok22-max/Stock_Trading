/**
 * Formatting utility functions for Stock Trading Application.
 * Member 5 - Dashboard & Integration.
 */

/**
 * Format a number as Indian Rupee (INR) currency.
 * Example: 150000.5 -> "₹1,50,000.50"
 */
export function formatINR(amount) {
  if (amount == null || isNaN(Number(amount))) return '₹0.00';
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(amount));
}

/**
 * Format a stock price with 2 decimal places in Indian format.
 * Example: 3542.5 -> "3,542.50"
 */
export function formatPrice(price) {
  if (price == null || isNaN(Number(price))) return '0.00';
  return new Intl.NumberFormat('en-IN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number(price));
}

/**
 * Format a percentage with leading sign and 2 decimal places.
 * Example: 2.941 -> "+2.94%", -1.25 -> "-1.25%"
 */
export function formatPercentage(value) {
  if (value == null || isNaN(Number(value))) return '0.00%';
  const num = Number(value);
  const sign = num > 0 ? '+' : '';
  return `${sign}${num.toFixed(2)}%`;
}

/**
 * Format date and time in Asia/Kolkata timezone with IST notation.
 */
export function formatDateTime(dateInput) {
  if (!dateInput) return '—';
  try {
    const date = new Date(dateInput);
    if (isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      timeZone: 'Asia/Kolkata',
    }).format(date) + ' IST';
  } catch (e) {
    return '—';
  }
}

/**
 * Format date only (DD MMM YYYY).
 */
export function formatDate(dateInput) {
  if (!dateInput) return '—';
  try {
    const date = new Date(dateInput);
    if (isNaN(date.getTime())) return '—';
    return new Intl.DateTimeFormat('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      timeZone: 'Asia/Kolkata',
    }).format(date);
  } catch (e) {
    return '—';
  }
}

/**
 * Formats a profit/loss amount returning text, class name, and polarity.
 */
export function formatProfitLoss(value) {
  if (value == null || isNaN(Number(value))) {
    return { text: '₹0.00', cssClass: 'text-neutral', isPositive: false, isNegative: false };
  }
  const num = Number(value);
  const formatted = formatINR(Math.abs(num));
  if (num > 0) {
    return { text: `+${formatted}`, cssClass: 'text-profit', isPositive: true, isNegative: false };
  } else if (num < 0) {
    return { text: `-${formatted}`, cssClass: 'text-loss', isPositive: false, isNegative: true };
  }
  return { text: formatted, cssClass: 'text-neutral', isPositive: false, isNegative: false };
}

/**
 * Format large numbers compactly (e.g. 1.5K, 2.3M, 4.5Cr).
 */
export function formatCompactNumber(num) {
  if (num == null || isNaN(Number(num))) return '0';
  return new Intl.NumberFormat('en-IN', {
    notation: 'compact',
    compactDisplay: 'short',
  }).format(Number(num));
}

