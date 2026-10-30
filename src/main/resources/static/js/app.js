/**
 * Stock Trading App - Module 1: User & Account Management
 * Frontend Application Controller
 */

const API_BASE = '/api';

// Application State
const AppState = {
  currentUser: null,
  account: null,
  token: null,
  transactions: [],
  currentTab: 'overview',
  transactionFilter: 'ALL',
};

// DOM ready
document.addEventListener('DOMContentLoaded', () => {
  initEventListeners();
  checkExistingSession();
});

function initEventListeners() {
  // Navigation tabs
  document.querySelectorAll('[data-tab]').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const tab = e.currentTarget.getAttribute('data-tab');
      switchTab(tab);
    });
  });

  // Auth toggle buttons
  const toRegisterBtn = document.getElementById('btn-to-register');
  if (toRegisterBtn) {
    toRegisterBtn.addEventListener('click', () => switchTab('register'));
  }
  const toLoginBtn = document.getElementById('btn-to-login');
  if (toLoginBtn) {
    toLoginBtn.addEventListener('click', () => switchTab('login'));
  }

  // Logout button
  const logoutBtn = document.getElementById('btn-logout');
  if (logoutBtn) {
    logoutBtn.addEventListener('click', handleLogout);
  }

  // Forms
  const loginForm = document.getElementById('form-login');
  if (loginForm) {
    loginForm.addEventListener('submit', handleLogin);
  }

  const registerForm = document.getElementById('form-register');
  if (registerForm) {
    registerForm.addEventListener('submit', handleRegister);
  }

  const depositForm = document.getElementById('form-deposit');
  if (depositForm) {
    depositForm.addEventListener('submit', handleDeposit);
  }

  const withdrawForm = document.getElementById('form-withdraw');
  if (withdrawForm) {
    withdrawForm.addEventListener('submit', handleWithdraw);
  }

  // Watchlist form
  const watchlistForm = document.getElementById('form-add-watchlist');
  if (watchlistForm) {
    watchlistForm.addEventListener('submit', handleAddWatchlist);
  }

  // Quick Amount chips for deposit
  document.querySelectorAll('.quick-amount-chip[data-deposit]').forEach(chip => {
    chip.addEventListener('click', () => {
      const val = chip.getAttribute('data-deposit');
      document.getElementById('deposit-amount').value = val;
    });
  });

  // Quick percent chips for withdraw
  document.querySelectorAll('.quick-amount-chip[data-withdraw-pct]').forEach(chip => {
    chip.addEventListener('click', () => {
      if (!AppState.account) return;
      const pct = parseFloat(chip.getAttribute('data-withdraw-pct'));
      const avail = parseFloat(AppState.account.availableBalance || 0);
      const calculated = (avail * pct).toFixed(2);
      document.getElementById('withdraw-amount').value = calculated;
    });
  });

  // Demo user quick login pills
  document.querySelectorAll('[data-demo-email]').forEach(pill => {
    pill.addEventListener('click', () => {
      const email = pill.getAttribute('data-demo-email');
      const pass = pill.getAttribute('data-demo-pass');
      document.getElementById('login-email').value = email;
      document.getElementById('login-password').value = pass;
      document.getElementById('form-login').dispatchEvent(new Event('submit'));
    });
  });

  // Transaction filters
  document.querySelectorAll('.txn-filter-btn').forEach(btn => {
    btn.addEventListener('click', (e) => {
      document.querySelectorAll('.txn-filter-btn').forEach(b => b.classList.remove('active'));
      e.currentTarget.classList.add('active');
      AppState.transactionFilter = e.currentTarget.getAttribute('data-filter');
      renderTransactions();
    });
  });

  // Admin action buttons
  const btnApproveKyc = document.getElementById('btn-approve-kyc');
  if (btnApproveKyc) {
    btnApproveKyc.addEventListener('click', handleApproveKyc);
  }

  const btnToggleBlock = document.getElementById('btn-toggle-block');
  if (btnToggleBlock) {
    btnToggleBlock.addEventListener('click', handleToggleBlock);
  }

  // Refresh balance button
  const btnRefresh = document.getElementById('btn-refresh-balance');
  if (btnRefresh) {
    btnRefresh.addEventListener('click', () => {
      refreshAccountData();
      showToast('Account data refreshed', 'info');
    });
  }
}

// Session check
function checkExistingSession() {
  const savedUser = localStorage.getItem('stock_user');
  const savedToken = localStorage.getItem('stock_token');
  if (savedUser && savedToken) {
    AppState.currentUser = JSON.parse(savedUser);
    AppState.token = savedToken;
    refreshAccountData().then(() => {
      updateNavForLoggedIn();
      switchTab('overview');
    }).catch(() => {
      handleLogout();
    });
  } else {
    updateNavForLoggedOut();
    switchTab('login');
  }
}

// Navigation & Tab Switching
function switchTab(tabId) {
  AppState.currentTab = tabId;

  // Update nav buttons
  document.querySelectorAll('[data-tab]').forEach(btn => {
    if (btn.getAttribute('data-tab') === tabId) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // Show/Hide sections
  const sections = ['login', 'register', 'overview', 'funds', 'transactions'];
  sections.forEach(s => {
    const el = document.getElementById(`view-${s}`);
    if (el) {
      if (s === tabId) {
        el.classList.remove('hidden');
      } else {
        el.classList.add('hidden');
      }
    }
  });

  // Tab specific refreshes
  if (AppState.currentUser && (tabId === 'overview' || tabId === 'funds')) {
    refreshAccountData();
  } else if (AppState.currentUser && tabId === 'transactions') {
    fetchTransactions();
  }
}

function updateNavForLoggedIn() {
  document.getElementById('nav-unauth').classList.add('hidden');
  document.getElementById('nav-auth').classList.remove('hidden');
  document.getElementById('user-display-name').textContent = AppState.currentUser.name;
  document.getElementById('user-avatar-initial').textContent = AppState.currentUser.name.charAt(0).toUpperCase();
}

function updateNavForLoggedOut() {
  document.getElementById('nav-unauth').classList.remove('hidden');
  document.getElementById('nav-auth').classList.add('hidden');
}

// Authentication Handlers
async function handleLogin(e) {
  e.preventDefault();
  const email = document.getElementById('login-email').value.trim();
  const password = document.getElementById('login-password').value;

  const submitBtn = document.getElementById('btn-submit-login');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Signing in...';

  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Login failed. Please check your credentials.');
    }

    AppState.currentUser = {
      userId: data.userId,
      name: data.name,
      email: data.email,
      role: data.role
    };
    AppState.token = data.token;
    AppState.account = data.account;

    localStorage.setItem('stock_user', JSON.stringify(AppState.currentUser));
    localStorage.setItem('stock_token', AppState.token);

    showToast(`Welcome back, ${data.name}!`, 'success');
    updateNavForLoggedIn();
    renderAccountOverview();
    switchTab('overview');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Sign In';
  }
}

async function handleRegister(e) {
  e.preventDefault();
  const name = document.getElementById('reg-name').value.trim();
  const email = document.getElementById('reg-email').value.trim();
  const password = document.getElementById('reg-password').value;
  const panNumber = document.getElementById('reg-pan').value.trim().toUpperCase();
  const phone = document.getElementById('reg-phone').value.trim();
  const linkedBankAccount = document.getElementById('reg-bank').value.trim();
  const initialDepositVal = document.getElementById('reg-deposit').value;
  const initialDeposit = initialDepositVal ? parseFloat(initialDepositVal) : null;

  const submitBtn = document.getElementById('btn-submit-register');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Creating Account...';

  try {
    const payload = {
      name,
      email,
      password,
      panNumber,
      phone,
      linkedBankAccount: linkedBankAccount || undefined,
      initialDeposit: initialDeposit || undefined
    };

    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok) {
      if (data.details) {
        const errorMessages = Object.values(data.details).join(', ');
        throw new Error(errorMessages || data.message);
      }
      throw new Error(data.message || 'Registration failed');
    }

    showToast('Registration successful! Please sign in with your credentials.', 'success');
    document.getElementById('login-email').value = email;
    document.getElementById('login-password').value = password;
    switchTab('login');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Complete Registration';
  }
}

function handleLogout() {
  if (AppState.token) {
    fetch(`${API_BASE}/auth/logout`, {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${AppState.token}` }
    }).catch(() => {});
  }
  AppState.currentUser = null;
  AppState.token = null;
  AppState.account = null;
  AppState.transactions = [];

  localStorage.removeItem('stock_user');
  localStorage.removeItem('stock_token');

  showToast('Logged out successfully', 'info');
  updateNavForLoggedOut();
  switchTab('login');
}

// Account Data Refresh
async function refreshAccountData() {
  if (!AppState.currentUser) return;
  try {
    const res = await fetch(`${API_BASE}/account/${AppState.currentUser.userId}`);
    if (!res.ok) {
      throw new Error('Failed to fetch account info');
    }
    const data = await res.json();
    AppState.account = data;
    renderAccountOverview();
  } catch (err) {
    console.error(err);
  }
}

// Render Dashboard & Overview
function renderAccountOverview() {
  if (!AppState.account) return;
  const acc = AppState.account;

  // Balances
  document.getElementById('ov-available').textContent = formatINR(acc.availableBalance);
  document.getElementById('ov-blocked').textContent = formatINR(acc.blockedBalance);
  document.getElementById('ov-total').textContent = formatINR(acc.totalBalance);

  // Status Badges
  const accountStatusEl = document.getElementById('ov-account-status');
  if (acc.status === 'ACTIVE') {
    accountStatusEl.className = 'badge badge-success';
    accountStatusEl.textContent = 'Active';
  } else {
    accountStatusEl.className = 'badge badge-danger';
    accountStatusEl.textContent = 'Blocked';
  }

  const kycStatusEl = document.getElementById('ov-kyc-status');
  if (acc.kycStatus === 'APPROVED') {
    kycStatusEl.className = 'badge badge-success';
    kycStatusEl.textContent = 'KYC Approved';
  } else if (acc.kycStatus === 'PENDING') {
    kycStatusEl.className = 'badge badge-warning';
    kycStatusEl.textContent = 'KYC Pending';
  } else {
    kycStatusEl.className = 'badge badge-danger';
    kycStatusEl.textContent = 'KYC Rejected';
  }

  const tradeStatusEl = document.getElementById('ov-trade-status');
  if (acc.canTrade) {
    tradeStatusEl.className = 'badge badge-success';
    tradeStatusEl.textContent = 'Trading Enabled';
  } else {
    tradeStatusEl.className = 'badge badge-warning';
    tradeStatusEl.textContent = 'Trading Locked';
  }

  // Profile fields
  document.getElementById('ov-user-id').textContent = acc.userId;
  document.getElementById('ov-account-id').textContent = acc.accountId;
  document.getElementById('ov-pan').textContent = acc.panNumber;
  document.getElementById('ov-phone').textContent = acc.phone;
  document.getElementById('ov-email').textContent = acc.userEmail;
  document.getElementById('ov-bank').textContent = acc.linkedBankAccount;

  // Forms source bank updates
  document.getElementById('deposit-source-bank').textContent = acc.linkedBankAccount;
  document.getElementById('withdraw-dest-bank').textContent = acc.linkedBankAccount;
  document.getElementById('withdraw-avail-display').textContent = formatINR(acc.availableBalance);

  // Admin button state
  const btnBlock = document.getElementById('btn-toggle-block');
  if (btnBlock) {
    btnBlock.textContent = acc.status === 'ACTIVE' ? 'Block Account (Test Rule)' : 'Unblock Account';
  }
  const btnKyc = document.getElementById('btn-approve-kyc');
  if (btnKyc) {
    btnKyc.style.display = acc.kycStatus === 'APPROVED' ? 'none' : 'inline-flex';
  }

  // Render Watchlist
  renderWatchlist(acc.watchlist || []);
}

function renderWatchlist(symbols) {
  const container = document.getElementById('watchlist-tags');
  const countEl = document.getElementById('watchlist-count');
  if (!container) return;

  countEl.textContent = `${symbols.length} / 20 stocks`;
  container.innerHTML = '';

  if (symbols.length === 0) {
    container.innerHTML = '<span class="text-muted" style="font-size:0.85rem;">No stocks in watchlist yet. Add one below!</span>';
    return;
  }

  symbols.forEach(sym => {
    const pill = document.createElement('div');
    pill.className = 'watchlist-pill';
    pill.innerHTML = `
      <span>${sym}</span>
      <button type="button" title="Remove ${sym}" onclick="handleRemoveWatchlist('${sym}')">&times;</button>
    `;
    container.appendChild(pill);
  });
}

// Deposit & Withdrawal Handlers
async function handleDeposit(e) {
  e.preventDefault();
  if (!AppState.currentUser) return;

  const amountVal = document.getElementById('deposit-amount').value;
  const description = document.getElementById('deposit-desc').value.trim();
  const amount = parseFloat(amountVal);

  if (isNaN(amount) || amount < 100 || amount > 100000) {
    showToast('Deposit amount must be between Rs. 100 and Rs. 1,00,000', 'error');
    return;
  }

  const submitBtn = document.getElementById('btn-submit-deposit');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Processing Deposit...';

  try {
    const res = await fetch(`${API_BASE}/account/deposit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: AppState.currentUser.userId,
        amount: amount,
        description: description || undefined
      })
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Deposit failed');
    }

    AppState.account = data;
    renderAccountOverview();
    showToast(`Successfully deposited Rs. ${amount.toLocaleString('en-IN')}`, 'success');
    document.getElementById('deposit-amount').value = '';
    document.getElementById('deposit-desc').value = '';
    switchTab('overview');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Deposit Funds';
  }
}

async function handleWithdraw(e) {
  e.preventDefault();
  if (!AppState.currentUser) return;

  const amountVal = document.getElementById('withdraw-amount').value;
  const description = document.getElementById('withdraw-desc').value.trim();
  const amount = parseFloat(amountVal);

  if (isNaN(amount) || amount <= 0) {
    showToast('Withdrawal amount must be greater than zero', 'error');
    return;
  }

  const avail = parseFloat(AppState.account?.availableBalance || 0);
  if (amount > avail) {
    showToast(`Cannot withdraw Rs. ${amount}. Only Rs. ${avail} available.`, 'error');
    return;
  }

  const submitBtn = document.getElementById('btn-submit-withdraw');
  submitBtn.disabled = true;
  submitBtn.textContent = 'Processing Withdrawal...';

  try {
    const res = await fetch(`${API_BASE}/account/withdraw`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: AppState.currentUser.userId,
        amount: amount,
        description: description || undefined
      })
    });

    const data = await res.json();
    if (!res.ok) {
      throw new Error(data.message || 'Withdrawal failed');
    }

    AppState.account = data;
    renderAccountOverview();
    showToast(`Successfully withdrew Rs. ${amount.toLocaleString('en-IN')}`, 'success');
    document.getElementById('withdraw-amount').value = '';
    document.getElementById('withdraw-desc').value = '';
    switchTab('overview');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = 'Withdraw Funds';
  }
}

// Transaction History
async function fetchTransactions() {
  if (!AppState.currentUser) return;
  const tbody = document.getElementById('txn-table-body');
  tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 2rem;">Loading transactions...</td></tr>';

  try {
    const res = await fetch(`${API_BASE}/account/${AppState.currentUser.userId}/transactions`);
    if (!res.ok) throw new Error('Failed to fetch transactions');
    AppState.transactions = await res.json();
    renderTransactions();
  } catch (err) {
    showToast(err.message, 'error');
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color: var(--danger);">${err.message}</td></tr>`;
  }
}

function renderTransactions() {
  const tbody = document.getElementById('txn-table-body');
  tbody.innerHTML = '';

  let list = AppState.transactions || [];
  if (AppState.transactionFilter === 'DEPOSIT') {
    list = list.filter(t => t.type === 'DEPOSIT');
  } else if (AppState.transactionFilter === 'WITHDRAWAL') {
    list = list.filter(t => t.type === 'WITHDRAWAL');
  }

  if (list.length === 0) {
    tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding: 2rem; color: var(--text-muted);">No transactions found.</td></tr>';
    return;
  }

  list.forEach(t => {
    const tr = document.createElement('tr');
    
    // Type badge
    let typeBadge = '';
    if (t.type === 'DEPOSIT') {
      typeBadge = `<span class="badge badge-success">&#8593; Deposit</span>`;
    } else if (t.type === 'WITHDRAWAL') {
      typeBadge = `<span class="badge badge-danger">&#8595; Withdrawal</span>`;
    } else {
      typeBadge = `<span class="badge badge-info">${t.type}</span>`;
    }

    // Status badge
    const statusBadge = t.status === 'SUCCESS' 
      ? `<span class="badge badge-success">Success</span>`
      : `<span class="badge badge-danger">Failed</span>`;

    const formattedTime = new Date(t.timestamp).toLocaleString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    });

    tr.innerHTML = `
      <td><code>${t.transactionId}</code></td>
      <td>${typeBadge}</td>
      <td style="font-weight:600; color: ${t.type === 'DEPOSIT' ? 'var(--success)' : 'var(--text-primary)'}">
        ${t.type === 'DEPOSIT' ? '+' : '-'}${formatINR(t.amount)}
      </td>
      <td>${formatINR(t.balanceAfter)}</td>
      <td>${statusBadge}</td>
      <td>${formattedTime}</td>
      <td style="font-size:0.82rem; color: var(--text-muted);">${t.description || '-'}</td>
    `;
    tbody.appendChild(tr);
  });
}

// Watchlist Actions
async function handleAddWatchlist(e) {
  e.preventDefault();
  if (!AppState.currentUser) return;
  const input = document.getElementById('watchlist-symbol');
  const symbol = input.value.trim().toUpperCase();
  if (!symbol) return;

  try {
    const res = await fetch(`${API_BASE}/account/${AppState.currentUser.userId}/watchlist/add`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ symbol })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Failed to add stock');

    AppState.account.watchlist = data;
    renderWatchlist(data);
    input.value = '';
    showToast(`Added ${symbol} to watchlist`, 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

window.handleRemoveWatchlist = async function(symbol) {
  if (!AppState.currentUser) return;
  try {
    const res = await fetch(`${API_BASE}/account/${AppState.currentUser.userId}/watchlist/remove`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ symbol })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Failed to remove stock');

    AppState.account.watchlist = data;
    renderWatchlist(data);
    showToast(`Removed ${symbol} from watchlist`, 'info');
  } catch (err) {
    showToast(err.message, 'error');
  }
};

// Admin Simulation Actions
async function handleApproveKyc() {
  if (!AppState.currentUser) return;
  try {
    const res = await fetch(`${API_BASE}/account/${AppState.currentUser.userId}/kyc/approve`, {
      method: 'POST'
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'KYC approval failed');

    AppState.account = data;
    renderAccountOverview();
    showToast('KYC successfully APPROVED! Trading eligibility unlocked.', 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function handleToggleBlock() {
  if (!AppState.currentUser) return;
  try {
    const res = await fetch(`${API_BASE}/account/${AppState.currentUser.userId}/toggle-block`, {
      method: 'POST'
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Action failed');

    AppState.account = data;
    renderAccountOverview();
    const isBlocked = data.status === 'BLOCKED';
    showToast(`Account status updated: ${data.status}`, isBlocked ? 'error' : 'success');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// Helpers
function formatINR(val) {
  if (val === null || val === undefined) return '₹0.00';
  const num = typeof val === 'number' ? val : parseFloat(val);
  return '₹' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `
    <div style="flex: 1; font-size: 0.88rem;">${message}</div>
    <button type="button" style="background:none; border:none; color:var(--text-muted); cursor:pointer; font-size:1.1rem;" onclick="this.parentElement.remove()">&times;</button>
  `;
  container.appendChild(toast);

  setTimeout(() => {
    if (toast.parentElement) {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(30px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }
  }, 4000);
}

