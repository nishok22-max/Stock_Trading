type Side = 'BUY' | 'SELL';
type OrderType = 'MARKET' | 'LIMIT';
type Status = 'PENDING' | 'EXECUTED' | 'CANCELLED' | 'REJECTED';
interface Account { userId: string; name: string; kycApproved: boolean; blocked: boolean; availableBalance: number; blockedBalance: number }
interface Holding { symbol: string; quantity: number; reservedQuantity: number; averageBuyPrice: number }
interface Stock { symbol: string; companyName: string; sector: string; price: number; previousClose: number; active: boolean }
interface Market { open: boolean; demoOpenMarket: boolean; serverTime: string; sessionClose: string; stocks: Stock[] }
interface Payload { symbol: string; side: Side; type: OrderType; quantity: number; limitPrice?: number }
interface Order extends Payload { id: string; userId: string; status: Status; reservedCash: number; reservedShares: number; executionPrice: number | null; brokerage: number; gst: number; tradeId: string | null; reason: string | null; createdAt: string; updatedAt: string; expiresAt: string }
interface Trade { id: string; orderId: string; symbol: string; side: Side; quantity: number; price: number; value: number; brokerage: number; gst: number; totalCharges: number; netCashFlow: number; realizedPnl: number; executedAt: string }
interface Charges { value: number; brokerage: number; gst: number; totalCharges: number; buyTotal: number; sellProceeds: number }
interface Preview { order: Payload; currentPrice: number; reservationPrice: number; charges: Charges; cashToReserve: number; sharesToReserve: number; executesImmediately: boolean; quotedAt: string; expiresAt: string }

const $ = <T extends HTMLElement = HTMLElement>(id: string) => document.getElementById(id) as T | null;
const input = (id: string) => $<HTMLInputElement>(id)!;
const button = (id: string) => $<HTMLButtonElement>(id)!;
const dialog = (id: string) => $<HTMLDialogElement>(id)!;
const set = (id: string, value: string) => { const e = $(id); if (e) e.textContent = value; };
const money = (n: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 }).format(n);
const time = (s: string) => new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kolkata' }).format(new Date(s)) + ' IST';
const base = (import.meta.env.PUBLIC_API_BASE_URL || '').replace(/\/$/, '');
const page = document.body.dataset.page;
let actor = sessionStorage.getItem('ledger-user') || '11111111-1111-4111-8111-111111111111';
let side: Side = 'BUY';
let symbol = 'TCS';
let market: Market | null = null;
let holdings: Holding[] = [];
let orders: Order[] = [];
let online = false;
let epoch = 0;
let formRevision = 0;
let refreshPromise: Promise<void> | null = null;
let review: { payload: Payload; preview: Preview; key: string; actor: string } | null = null;
let submitting = false;
let cancelId: string | null = null;
let toastTimer: ReturnType<typeof setTimeout>;
let lastOrders = '';

class ApiError extends Error { constructor(message: string, public status: number) { super(message); } }
async function api<T>(path: string, init: RequestInit = {}, user = actor): Promise<T> {
  let response: Response;
  try {
    response = await fetch(base + path, { ...init, headers: { 'Content-Type': 'application/json', 'X-User-Id': user, ...init.headers }, signal: AbortSignal.timeout(12000) });
  } catch { throw new ApiError('Cannot reach the trading service. Check that the backend is running, then retry.', 0); }
  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new ApiError(data?.message || `Request failed (${response.status}). Please retry.`, response.status);
  }
  return response.json() as Promise<T>;
}
function message(error: unknown) { return error instanceof Error ? error.message : 'The operation could not be completed.'; }
function alertAt(id: string, text: string | null) { const e = $(id); if (e) { e.textContent = text || ''; e.hidden = !text; } }
function toast(text: string) { const e = $('toast')!; clearTimeout(toastTimer); e.textContent = text; e.hidden = false; toastTimer = setTimeout(() => { e.hidden = true; }, 5500); }
function cell(text: string, className = '') { const e = document.createElement('td'); e.textContent = text; e.className = className; return e; }
function node<K extends keyof HTMLElementTagNameMap>(tag: K, text = '', className = '') { const e = document.createElement(tag); e.textContent = text; e.className = className; return e; }
function detailList(id: string, details: [string, string][]) { const root = $(id)!; root.replaceChildren(...details.map(([label, value]) => { const row = node('div'); row.append(node('dt', label), node('dd', value)); return row; })); }
function empty(root: HTMLElement, text: string, columns: number) {
  const row = node('tr'); const td = cell('', 'empty-cell'); td.colSpan = columns;
  td.append(node('p', text)); const link = node('a', 'Go to the trading desk'); link.href = '/'; td.append(link); row.append(td); root.replaceChildren(row);
}

async function refresh(force = false) {
  if (refreshPromise && !force) return refreshPromise;
  const generation = epoch; const user = actor;
  const task = (async () => {
    try {
      const [a, m, o, t] = await Promise.all([
        api<{ account: Account; holdings: Holding[] }>('/api/trading/account', {}, user),
        api<Market>('/api/trading/market', {}, user),
        api<Order[]>(`/api/orders/user/${user}`, {}, user),
        page === 'trades' ? api<Trade[]>(`/api/trades/user/${user}`, {}, user) : Promise.resolve([] as Trade[]),
      ]);
      if (generation !== epoch) return;
      const oldPrice = market?.stocks.find(s => s.symbol === symbol)?.price;
      holdings = a.holdings; market = m; orders = o; online = true;
      if (review && oldPrice !== undefined && oldPrice !== m.stocks.find(s => s.symbol === symbol)?.price && !submitting) {
        alertAt('review-error', 'The quote changed. Close this review and review the order again.');
        button('confirm-order').disabled = true;
      }
      alertAt('connection-error', null);
      $('demo-banner')!.hidden = !m.demoOpenMarket;
      set('available-balance', money(a.account.availableBalance)); set('blocked-balance', money(a.account.blockedBalance));
      const count = o.filter(item => item.status === 'PENDING').length;
      set('pending-metric', String(count).padStart(2, '0')); set('pending-count', String(count));
      document.querySelectorAll('#balances .skeleton').forEach(e => e.classList.remove('skeleton'));
      $('balances')!.setAttribute('aria-busy', 'false');
      set('account-status', !a.account.kycApproved ? 'KYC pending' : a.account.blocked ? 'Buys blocked' : 'Ready to trade');
      set('account-status-note', !a.account.kycApproved ? 'Approval needed before trading' : a.account.blocked ? 'Sell orders are still permitted' : 'KYC verified');
      set('profile-name', a.account.name); set('profile-initials', a.account.name.split(' ').map(x => x[0]).slice(0, 2).join(''));
      const status = $('market-status')!; status.replaceChildren(node('span', m.open ? 'Market open' : 'Market closed'), node('span', '', 'status-dot')); status.classList.toggle('open', m.open);
      set('sync-status', `Updated ${time(m.serverTime)} · Refreshes every 5s`);
      renderStocks(); renderHoldings(); estimate();
      const serialized = JSON.stringify(page === 'trades' ? t : o);
      if (serialized !== lastOrders) { renderHistory(t); lastOrders = serialized; }
    } catch (e) {
      if (generation !== epoch) return;
      online = false; alertAt('connection-error', message(e)); set('sync-status', 'Connection interrupted · Displayed data may be stale');
      if (review && !submitting) { alertAt('review-error', 'Connection interrupted. Close this review and retry after reconnecting.'); button('confirm-order').disabled = true; }
    } finally { if (generation === epoch && $('review-order')) button('review-order').disabled = !online; }
  })();
  refreshPromise = task;
  try { await task; } finally { if (refreshPromise === task) refreshPromise = null; }
}
function renderStocks() {
  const list = $('stock-list'); if (!list || !market) return;
  const query = input('stock-search').value.toLowerCase();
  const selected = market.stocks.find(s => s.symbol === symbol);
  if (!selected) symbol = market.stocks[0]?.symbol || '';
  const stocks = market.stocks.filter(s => `${s.symbol} ${s.companyName} ${s.sector}`.toLowerCase().includes(query));
  list.replaceChildren(...stocks.map(stock => {
    const row = node('button', '', 'stock-row' + (stock.symbol === symbol ? ' selected' : ''));
    row.type = 'button'; row.dataset.symbol = stock.symbol; row.setAttribute('aria-pressed', String(stock.symbol === symbol));
    const identity = node('span', '', 'stock-identity'); const copy = node('span', '', 'stock-copy');
    copy.append(node('strong', stock.symbol), node('span', stock.companyName));
    identity.append(node('span', stock.symbol.slice(0, 2), 'stock-monogram'), copy);
    const pricing = node('span', '', 'stock-pricing'); const change = (stock.price - stock.previousClose) / stock.previousClose * 100;
    pricing.append(node('strong', money(stock.price)), node('span', stock.active ? `${change >= 0 ? '+' : ''}${change.toFixed(2)}%` : 'Suspended', change >= 0 ? 'positive' : 'negative'));
    row.append(identity, pricing); row.addEventListener('click', () => selectStock(stock.symbol)); return row;
  }));
  if (!stocks.length) list.append(node('p', 'No matching stocks. Try another name or symbol.', 'empty-cell'));
}
function selectStock(next: string) {
  symbol = next; invalidateReview();
  const stock = market?.stocks.find(s => s.symbol === symbol);
  if (stock && input('limit-price')) input('limit-price').value = stock.price.toFixed(2);
  renderStocks(); estimate();
}
function setSide(next: Side) {
  side = next; invalidateReview();
  for (const s of ['BUY', 'SELL'] as const) { const e = button(s === 'BUY' ? 'side-buy' : 'side-sell'); e.classList.toggle('selected', side === s); e.setAttribute('aria-pressed', String(side === s)); }
  estimate();
}
function invalidateReview() { formRevision++; review = null; alertAt('form-error', null); }
function payload(): Payload {
  const type = input('order-type').value as OrderType; const qty = Number(input('quantity').value);
  if (!Number.isInteger(qty) || qty < 1 || qty > 1000) throw new Error('Enter a whole-number quantity from 1 to 1,000.');
  if (!symbol) throw new Error('Select a stock first.');
  const r: Payload = { symbol, side, type, quantity: qty };
  if (type === 'LIMIT') {
    const price = input('limit-price').value;
    if (!/^\d+(\.\d{1,2})?$/.test(price) || Number(price) < 0.01 || Number(price) > 999999999.99) throw new Error('Enter a positive limit price with at most two decimal places.');
    r.limitPrice = Number(price);
  }
  return r;
}
function estimate() {
  if (!$('order-form') || !market) return;
  const stock = market.stocks.find(s => s.symbol === symbol); if (!stock) return;
  set('ticket-symbol', stock.symbol); set('ticket-company', stock.companyName); set('ticket-price', money(stock.price));
  const h = holdings.find(h => h.symbol === symbol);
  set('holding-hint', `${h ? h.quantity - h.reservedQuantity : 0} shares available to sell${h?.reservedQuantity ? ` · ${h.reservedQuantity} reserved` : ''}`);
  const isLimit = input('order-type').value === 'LIMIT';
  $('limit-field')!.hidden = !isLimit; input('limit-price').required = isLimit; input('limit-price').disabled = !isLimit;
  set('type-help', isLimit ? `${side === 'BUY' ? 'Buys at or below' : 'Sells at or above'} your limit. Expires at session close.` : 'Executes immediately at the current simulated price.');
  set('review-order', `Review ${side.toLowerCase()} order →`); set('estimate-label', side === 'BUY' ? (isLimit ? 'Cash to reserve' : 'Estimated total') : 'Estimated proceeds');
  const quantity = Number(input('quantity').value); const price = isLimit ? Number(input('limit-price').value) : stock.price;
  if (!Number.isInteger(quantity) || quantity < 1 || quantity > 1000 || !Number.isFinite(price) || price <= 0) {
    for (const id of ['estimated-value', 'estimated-charges', 'estimated-total']) set(id, '—'); return;
  }
  // Display estimate in integer paise; confirmation always uses authoritative server calculations.
  const value = Math.round(price * 100) * quantity; const brokerage = Math.min(2000, Math.round(value * 0.0005)); const charges = brokerage + Math.round(brokerage * 0.18);
  set('estimated-value', money(value / 100)); set('estimated-charges', money(charges / 100)); set('estimated-total', money((value + (side === 'BUY' ? charges : -charges)) / 100));
}
function renderHoldings() {
  const body = $('holdings-body'); if (!body || !market) return;
  if (!holdings.length) { empty(body, 'Your holdings will appear after your first buy trade.', 8); return; }
  body.replaceChildren(...holdings.map(h => {
    const price = market!.stocks.find(s => s.symbol === h.symbol)?.price;
    const pnl = price === undefined ? null : (price - h.averageBuyPrice) * h.quantity;
    const row = node('tr'); row.append(cell(h.symbol, 'stock-cell'), cell(String(h.quantity), 'number'), cell(String(h.reservedQuantity), 'number'), cell(String(h.quantity - h.reservedQuantity), 'number'), cell(money(h.averageBuyPrice), 'number'), cell(price === undefined ? '—' : money(price), 'number'), cell(pnl === null ? '—' : money(pnl), `number ${pnl !== null && pnl < 0 ? 'negative' : 'positive'}`));
    const action = cell(''); const sell = node('button', 'Sell', 'table-action'); sell.type = 'button'; sell.disabled = h.quantity === h.reservedQuantity;
    sell.setAttribute('aria-label', `Sell ${h.symbol}`); sell.addEventListener('click', () => { selectStock(h.symbol); setSide('SELL'); $('order-form')!.scrollIntoView({ behavior: 'smooth', block: 'center' }); input('quantity').focus({ preventScroll: true }); }); action.append(sell); row.append(action); return row;
  }));
}
function renderHistory(trades: Trade[]) {
  const body = $('history-body'); if (!body) return;
  const head = $('history-head')!;
  if (page === 'trades') {
    const headings = ['Trade / order', 'Stock / side', 'Qty', 'Price', 'Value', 'Brokerage', 'GST', 'Net cash flow', 'Realized P/L', 'Executed'];
    head.replaceChildren(header(headings)); set('history-summary', `${trades.length} execution${trades.length === 1 ? '' : 's'} · Charges are included in net cash flow. Realized P/L excludes charges.`);
    if (!trades.length) { empty(body, 'No executions yet. A completed order creates a trade here.', headings.length); return; }
    body.replaceChildren(...trades.map(t => {
      const row = node('tr'); const id = cell(t.id.slice(0, 8), 'stock-cell'); id.title = t.id; id.append(node('span', `Order ${t.orderId.slice(0, 8)}`, 'subcell'));
      row.append(id, cell(`${t.symbol} · ${t.side}`), cell(String(t.quantity), 'number'), cell(money(t.price), 'number'), cell(money(t.value), 'number'), cell(money(t.brokerage), 'number'), cell(money(t.gst), 'number'), cell(money(t.netCashFlow), 'number'), cell(t.side === 'SELL' ? money(t.realizedPnl) : '—', 'number'), cell(time(t.executedAt))); return row;
    })); return;
  }
  const filter = document.querySelector<HTMLElement>('[data-filter]')?.dataset.filter || 'ALL';
  const filtered = orders.filter(o => filter === 'ALL' || o.status === filter);
  head.replaceChildren(header(['Stock / order', 'Side / type', 'Qty', 'Price', 'Reservation / fees', 'Status', 'Placed', 'Action']));
  set('history-summary', `${filtered.length} order${filtered.length === 1 ? '' : 's'}${filter === 'PENDING' ? ' · Cash and shares stay reserved until execution, cancellation or expiry.' : ' · Times shown in IST.'}`);
  if (!filtered.length) { empty(body, filter === 'PENDING' ? 'No pending orders. Limit orders wait here when the market has not reached your price.' : filter === 'EXECUTED' ? 'No executed orders yet. Your completed orders will appear here.' : 'Your order history starts with your first trade.', 8); return; }
  body.replaceChildren(...filtered.map(o => {
    const row = node('tr'); const stock = cell(o.symbol, 'stock-cell'); stock.append(node('span', o.id.slice(0, 8), 'subcell')); stock.title = o.id;
    const type = cell(o.side); type.append(node('span', o.type, 'subcell'));
    const price = cell(money(o.executionPrice ?? o.limitPrice ?? 0), 'number'); price.append(node('span', o.status === 'EXECUTED' ? 'Executed price' : o.type === 'LIMIT' ? 'Limit price' : 'Market', 'subcell'));
    const fees = cell(o.status === 'PENDING' ? o.side === 'BUY' ? money(o.reservedCash) : `${o.reservedShares} shares` : o.status === 'EXECUTED' ? money(o.brokerage + o.gst) : 'Released', 'number');
    fees.append(node('span', o.status === 'PENDING' ? `Expires ${time(o.expiresAt)}` : o.status === 'EXECUTED' ? `Brokerage ${money(o.brokerage)} + GST ${money(o.gst)}` : '', 'subcell'));
    const status = cell(''); status.append(node('span', o.status, `status-badge ${o.status.toLowerCase()}`)); if (o.reason) status.append(node('span', o.reason, 'subcell'));
    const action = cell(''); if (o.status === 'PENDING') { const cancel = node('button', 'Cancel', 'table-action'); cancel.type = 'button'; cancel.dataset.cancel = o.id; cancel.setAttribute('aria-label', `Cancel ${o.symbol} order ${o.id.slice(0, 8)}`); action.append(cancel); }
    row.append(stock, type, cell(String(o.quantity), 'number'), price, fees, status, cell(time(o.createdAt)), action); return row;
  }));
}
function header(labels: string[]) { const row = node('tr'); labels.forEach(label => row.append(node('th', label))); return row; }
function attemptKey(data: Payload): string {
  const signature = `${actor}:${JSON.stringify(data)}`;
  try { const prior = JSON.parse(sessionStorage.getItem('ledger-attempt') || 'null'); if (prior?.signature === signature) return prior.key; } catch { /* Ignore an invalid local draft. */ }
  const key = crypto.randomUUID(); sessionStorage.setItem('ledger-attempt', JSON.stringify({ signature, key })); return key;
}
function setSubmitting(value: boolean) { submitting = value; $<HTMLSelectElement>('account-select')!.disabled = value; }

$('order-form')?.addEventListener('submit', async event => {
  event.preventDefault(); alertAt('form-error', null); const revision = formRevision; const user = actor; button('review-order').disabled = true; set('review-order', 'Checking order…');
  try {
    const data = payload(); const preview = await api<Preview>('/api/orders/preview', { method: 'POST', body: JSON.stringify(data) });
    if (revision !== formRevision || user !== actor) return;
    review = { payload: data, preview, key: attemptKey(data), actor: user };
    set('review-subtitle', `${data.side === 'BUY' ? 'Buy' : 'Sell'} ${data.quantity} ${data.symbol} ${data.quantity === 1 ? 'share' : 'shares'} · ${data.type.toLowerCase()} order`);
    detailList('review-details', [
      ['Current price', money(preview.currentPrice)], ['Price for reservation', money(preview.reservationPrice)],
      ['Trade value at reservation price', money(preview.charges.value)], ['Brokerage', money(preview.charges.brokerage)], ['GST (18% of brokerage)', money(preview.charges.gst)],
      [data.side === 'BUY' ? 'Cash to reserve' : 'Shares to reserve', data.side === 'BUY' ? money(preview.cashToReserve) : String(preview.sharesToReserve)],
      ['Session expiry', time(preview.expiresAt)],
    ]);
    set('review-message', preview.executesImmediately ? 'This order is eligible for immediate execution at the current quote. A fresh quote is used when you confirm.' : `This order will stay pending until the market price is ${data.side === 'BUY' ? 'at or below' : 'at or above'} your limit.`);
    alertAt('review-error', null); button('confirm-order').disabled = false; set('confirm-order', 'Confirm order'); dialog('review-dialog').showModal();
  } catch (e) { if (revision === formRevision && user === actor) alertAt('form-error', message(e)); }
  finally { button('review-order').disabled = !online; estimate(); }
});
button('confirm-order')?.addEventListener('click', async () => {
  if (!review || submitting) return; const current = review; setSubmitting(true); button('confirm-order').disabled = true; set('confirm-order', 'Placing order…'); alertAt('review-error', null);
  try {
    const order = await api<Order>('/api/orders', { method: 'POST', body: JSON.stringify(current.payload), headers: { 'Idempotency-Key': current.key } }, current.actor);
    sessionStorage.removeItem('ledger-attempt'); dialog('review-dialog').close(); review = null;
    const executed = order.status === 'EXECUTED'; const pending = order.status === 'PENDING';
    set('confirmation-title', executed ? 'Order executed' : pending ? 'Limit order placed' : 'Order rejected');
    set('confirmation-mark', executed ? '✓' : pending ? '↗' : '!');
    set('confirmation-message', order.reason || (executed ? `${order.side === 'BUY' ? 'Bought' : 'Sold'} ${order.quantity} ${order.symbol} shares at ${money(order.executionPrice!)} per share.` : `${order.quantity} ${order.symbol} shares will ${order.side === 'BUY' ? 'buy' : 'sell'} when your limit is reached.`));
    detailList('confirmation-details', [['Order ID', order.id], ['Status', order.status], ...(executed ? [['Brokerage + GST', money(order.brokerage + order.gst)]] as [string, string][] : pending ? [['Reserved', order.side === 'BUY' ? money(order.reservedCash) : `${order.reservedShares} shares`]] as [string, string][] : [])]);
    const link = $<HTMLAnchorElement>('confirmation-link')!; link.href = executed ? '/orders/executed' : pending ? '/orders/pending' : '/orders'; link.textContent = executed ? 'View executed orders' : pending ? 'View pending orders' : 'View order history';
    dialog('confirmation-dialog').showModal(); await refresh(true);
  } catch (e) { alertAt('review-error', message(e) + ' You can retry safely with the same request.'); button('confirm-order').disabled = false; set('confirm-order', 'Retry confirmation'); }
  finally { setSubmitting(false); }
});
$('close-review')?.addEventListener('click', () => { if (!submitting) dialog('review-dialog').close(); });
$('close-confirmation')?.addEventListener('click', () => dialog('confirmation-dialog').close());
$('side-buy')?.addEventListener('click', () => setSide('BUY'));
$('side-sell')?.addEventListener('click', () => setSide('SELL'));
for (const id of ['quantity', 'limit-price', 'order-type']) $(id)?.addEventListener('input', () => { invalidateReview(); estimate(); });
$('stock-search')?.addEventListener('input', renderStocks);

$('history-body')?.addEventListener('click', event => {
  const target = (event.target as HTMLElement).closest<HTMLButtonElement>('[data-cancel]'); if (!target) return;
  cancelId = target.dataset.cancel!; const order = orders.find(o => o.id === cancelId); if (!order) return;
  set('cancel-description', `${order.side} ${order.quantity} ${order.symbol} shares · limit ${money(order.limitPrice!)}`); alertAt('cancel-error', null); button('confirm-cancel').disabled = false; set('confirm-cancel', 'Cancel order'); dialog('cancel-dialog').showModal();
});
$('confirm-cancel')?.addEventListener('click', async () => {
  if (!cancelId || submitting) return; setSubmitting(true); button('confirm-cancel').disabled = true; set('confirm-cancel', 'Cancelling…');
  try { await api(`/api/orders/${cancelId}/cancel`, { method: 'POST' }); dialog('cancel-dialog').close(); toast('Order cancelled. Your reservation has been released.'); await refresh(true); }
  catch (e) { alertAt('cancel-error', message(e)); button('confirm-cancel').disabled = false; set('confirm-cancel', 'Retry cancellation'); await refresh(true); }
  finally { setSubmitting(false); }
});
$('close-cancel')?.addEventListener('click', () => { if (!submitting) dialog('cancel-dialog').close(); });
document.querySelectorAll('dialog').forEach(d => d.addEventListener('cancel', e => { if (submitting) e.preventDefault(); }));
$('refresh-history')?.addEventListener('click', async () => { button('refresh-history').disabled = true; try { lastOrders = ''; await refresh(true); } finally { button('refresh-history').disabled = false; } });
$<HTMLSelectElement>('account-select')!.addEventListener('change', event => {
  actor = (event.target as HTMLSelectElement).value; sessionStorage.setItem('ledger-user', actor); epoch++; formRevision++; review = null; lastOrders = ''; online = false;
  document.querySelectorAll('dialog[open]').forEach(d => (d as HTMLDialogElement).close());
  for (const id of ['available-balance', 'blocked-balance', 'pending-metric']) set(id, '—');
  set('account-status', 'Loading'); set('account-status-note', 'Checking account eligibility');
  if ($('review-order')) button('review-order').disabled = true;
  if ($('history-body')) empty($('history-body')!, 'Loading this account’s activity…', 8);
  if ($('holdings-body')) empty($('holdings-body')!, 'Loading this account’s holdings…', 8);
  void refresh(true);
});
async function start() {
  try {
    const users = await api<{ userId: string; name: string; kycApproved: boolean; blocked: boolean }[]>('/api/demo/users');
    const select = $<HTMLSelectElement>('account-select')!;
    select.replaceChildren(...users.map(u => { const option = node('option', u.name + (!u.kycApproved ? ' · KYC pending' : u.blocked ? ' · Buys blocked' : '')); option.value = u.userId; return option; }));
    if (!users.some(u => u.userId === actor)) actor = users[0]?.userId || actor;
    select.value = actor;
  } catch { const select = $<HTMLSelectElement>('account-select')!; select.value = actor; }
  await refresh();
}
void start();
const interval = setInterval(() => { if (!document.hidden && !submitting) void refresh(); }, 5000);
document.addEventListener('visibilitychange', () => { if (!document.hidden && !submitting) void refresh(); });
window.addEventListener('pagehide', () => clearInterval(interval));
