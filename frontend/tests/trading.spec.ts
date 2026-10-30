import { test, expect } from '@playwright/test';

const user = '11111111-1111-4111-8111-111111111111';
const headers = { 'X-User-Id': user };
const backend = process.env.BACKEND_URL || 'http://127.0.0.1:8080';

test.beforeEach(async ({ page, request }) => {
  // These tests run against a local, explicitly extended demo session.
  const market = await request.get(`${backend}/api/trading/market`);
  expect((await market.json()).demoOpenMarket).toBe(true);
  const reset = await request.put(`${backend}/api/demo/stocks/INFY/price`, { data: { price: 1524.8 } });
  expect(reset.ok()).toBeTruthy();
  await page.goto('/');
  await expect(page.locator('#account-status')).toHaveText('Ready to trade');
});

test('market buy review, execution and history', async ({ page }) => {
  await page.locator('[data-symbol="INFY"]').click();
  await page.locator('#quantity').fill('2');
  await page.getByRole('button', { name: /Review buy order/ }).click();
  await expect(page.locator('#review-dialog')).toBeVisible();
  await expect(page.locator('#review-subtitle')).toContainText('Buy 2 INFY');
  await page.getByRole('button', { name: 'Confirm order', exact: true }).click();
  await expect(page.locator('#confirmation-title')).toHaveText('Order executed');
  const id = await page.locator('#confirmation-details dd').first().innerText();
  await page.getByRole('link', { name: 'View executed orders' }).click();
  const row = page.getByRole('row').filter({ hasText: id.slice(0, 8) });
  await expect(row).toContainText('INFY'); await expect(row).toContainText('EXECUTED');
  await expect(row).toContainText('1,524.80');
});

test('sell from holdings updates available quantity', async ({ page, request }) => {
  const before = await (await request.get(`${backend}/api/trading/account`, { headers })).json();
  const quantity = before.holdings.find((h: { symbol: string }) => h.symbol === 'INFY').quantity;
  await page.getByRole('button', { name: 'Sell INFY', exact: true }).click();
  await expect(page.locator('#side-sell')).toHaveAttribute('aria-pressed', 'true');
  await page.locator('#quantity').fill('1');
  await page.getByRole('button', { name: /Review sell order/ }).click();
  await page.getByRole('button', { name: 'Confirm order', exact: true }).click();
  await expect(page.locator('#confirmation-title')).toHaveText('Order executed');
  const after = await (await request.get(`${backend}/api/trading/account`, { headers })).json();
  expect(after.holdings.find((h: { symbol: string }) => h.symbol === 'INFY').quantity).toBe(quantity - 1);
});

test('pending limit reserves cash and cancellation releases it', async ({ page, request }) => {
  const before = await (await request.get(`${backend}/api/trading/account`, { headers })).json();
  await page.locator('[data-symbol="INFY"]').click();
  await page.locator('#order-type').selectOption('LIMIT');
  await page.locator('#limit-price').fill('1470');
  await page.locator('#quantity').fill('2');
  await page.getByRole('button', { name: /Review buy order/ }).click();
  await page.getByRole('button', { name: 'Confirm order', exact: true }).click();
  await expect(page.locator('#confirmation-title')).toHaveText('Limit order placed');
  const id = await page.locator('#confirmation-details dd').first().innerText();
  await page.getByRole('link', { name: 'View pending orders' }).click();
  const row = page.getByRole('row').filter({ hasText: id.slice(0, 8) });
  await expect(row).toContainText('PENDING'); await row.getByRole('button', { name: /Cancel/ }).click();
  await page.getByRole('button', { name: 'Cancel order', exact: true }).click();
  await expect(page.locator('#cancel-dialog')).not.toBeVisible(); await expect(row).toHaveCount(0);
  const after = await (await request.get(`${backend}/api/trading/account`, { headers })).json();
  expect(after.account.availableBalance).toBe(before.account.availableBalance);
  expect(after.account.blockedBalance).toBe(before.account.blockedBalance);
});

test('quote crossing executes a pending limit and creates one trade', async ({ page, request }) => {
  await page.locator('[data-symbol="INFY"]').click(); await page.locator('#order-type').selectOption('LIMIT'); await page.locator('#limit-price').fill('1500');
  await page.getByRole('button', { name: /Review buy order/ }).click(); await page.getByRole('button', { name: 'Confirm order', exact: true }).click();
  await expect(page.locator('#confirmation-title')).toHaveText('Limit order placed');
  const id = await page.locator('#confirmation-details dd').first().innerText();
  const update = await request.put(`${backend}/api/demo/stocks/INFY/price`, { data: { price: 1490 } }); expect(update.ok()).toBeTruthy();
  await page.goto('/orders/executed'); const row = page.getByRole('row').filter({ hasText: id.slice(0, 8) }); await expect(row).toContainText('1,490.00');
  const trades = await (await request.get(`${backend}/api/trades/user/${user}`, { headers })).json();
  expect(trades.filter((t: { orderId: string }) => t.orderId === id)).toHaveLength(1);
});

test('blocked buys, unapproved KYC and overselling show inline errors', async ({ page }) => {
  await page.locator('#account-select').selectOption('22222222-2222-4222-8222-222222222222');
  await expect(page.locator('#account-status')).toHaveText('Buys blocked');
  await page.getByRole('button', { name: /Review buy order/ }).click(); await expect(page.locator('#form-error')).toContainText('Blocked accounts');
  await page.locator('#account-select').selectOption('33333333-3333-4333-8333-333333333333');
  await expect(page.locator('#account-status')).toHaveText('KYC pending');
  await page.getByRole('button', { name: /Review buy order/ }).click(); await expect(page.locator('#form-error')).toContainText('KYC');
  await page.locator('#account-select').selectOption(user); await expect(page.locator('#account-status')).toHaveText('Ready to trade');
  await page.locator('#side-sell').click(); await page.locator('#quantity').fill('1000');
  await page.getByRole('button', { name: /Review sell order/ }).click(); await expect(page.locator('#form-error')).toContainText('unreserved shares');
});

test('network error preserves confirmation key for a safe retry', async ({ page, request }) => {
  await page.getByRole('button', { name: /Review buy order/ }).click();
  const keys: string[] = []; let first = true;
  await page.route('**/api/orders', async route => {
    if (route.request().method() !== 'POST') return route.continue();
    keys.push(route.request().headers()['idempotency-key']);
    if (first) { first = false; await route.fetch(); await route.abort(); } else await route.continue();
  });
  await page.getByRole('button', { name: 'Confirm order', exact: true }).click();
  await expect(page.locator('#review-error')).toContainText('retry safely');
  await page.getByRole('button', { name: 'Retry confirmation' }).click();
  await expect(page.locator('#confirmation-title')).toHaveText('Order executed'); expect(keys).toHaveLength(2); expect(keys[0]).toBe(keys[1]);
  const id = await page.locator('#confirmation-details dd').first().innerText();
  const trades = await (await request.get(`${backend}/api/trades/user/${user}`, { headers })).json();
  expect(trades.filter((t: { orderId: string }) => t.orderId === id)).toHaveLength(1);
});

test('mobile layout and keyboard review remain usable', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await expect(page.locator('#order-form')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.locator('#quantity').focus(); await page.keyboard.press('Enter');
  await expect(page.locator('#review-dialog')).toBeVisible(); await page.keyboard.press('Escape'); await expect(page.locator('#review-dialog')).not.toBeVisible();
  await page.screenshot({ path: 'test-results/mobile-desk.png', fullPage: true });
});

test('trade history, empty search and connection failure states', async ({ page }) => {
  await page.locator('#stock-search').fill('no-such-company'); await expect(page.locator('#stock-list')).toContainText('No matching stocks');
  await page.goto('/trades'); await expect(page.locator('#history-head')).toContainText('Net cash flow');
  await page.route('**/api/trading/account', route => route.abort()); await page.getByRole('button', { name: 'Refresh', exact: true }).click();
  await expect(page.locator('#connection-error')).toBeVisible();
});
