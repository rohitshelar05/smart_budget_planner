(() => {
  'use strict';

  const $ = (sel) => document.querySelector(sel);
  const state = { currency: 'USD', period: toMonthValue(new Date()), charts: {}, budgets: [], transactions: [] };
  const PALETTE = ['#5b5bf0', '#10b981', '#f59e0b', '#f43f5e', '#06b6d4', '#a855f7', '#84cc16', '#fb7185', '#14b8a6', '#eab308'];

  function toMonthValue(d) { return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0'); }
  function today() { const d = new Date(); return toMonthValue(d) + '-' + String(d.getDate()).padStart(2, '0'); }
  function parts(period) { const [y, m] = period.split('-').map(Number); return { year: y, month: m }; }
  function periodQuery() { const p = parts(state.period); return 'year=' + p.year + '&month=' + p.month; }
  function esc(v) {
    return String(v == null ? '' : v).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  }
  function money(n) {
    try { return new Intl.NumberFormat(undefined, { style: 'currency', currency: state.currency }).format(Number(n)); }
    catch (e) { return Number(n).toFixed(2); }
  }
  function monthLabel(period) {
    const p = parts(period);
    return new Date(p.year, p.month - 1, 1).toLocaleDateString(undefined, { month: 'long', year: 'numeric' });
  }

  async function api(path, options) {
    const res = await fetch('/api' + path, Object.assign({ headers: { 'Content-Type': 'application/json' } }, options));
    if (res.status === 204) { return null; }
    let body = null;
    try { body = await res.json(); } catch (e) { /* non-JSON error body */ }
    if (!res.ok) {
      const err = new Error((body && body.message) || 'Request failed (' + res.status + ')');
      err.fieldErrors = (body && body.fieldErrors) || {};
      throw err;
    }
    return body;
  }

  let toastTimer;
  function toast(message, isError) {
    const el = $('#toast');
    el.textContent = message;
    el.className = 'toast' + (isError ? ' error' : '');
    el.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.hidden = true; }, 3500);
  }

  // ---------- rendering ----------
  function renderSummary(s) {
    $('#sumIncome').textContent = money(s.totalIncome);
    $('#sumExpense').textContent = money(s.totalExpense);
    const net = $('#sumNet');
    net.textContent = money(s.net);
    net.className = Number(s.net) >= 0 ? 'pos' : 'neg';
    $('#sumRate').textContent = Number(s.savingsRate).toFixed(1) + '%';
  }

  function renderAlerts(alerts) {
    const box = $('#alerts');
    box.innerHTML = alerts.map((a) => {
      const exceeded = a.alertLevel === 'EXCEEDED';
      const text = exceeded
        ? esc(a.category) + ' budget exceeded: ' + money(a.spent) + ' spent of ' + money(a.limitAmount) + ' (' + a.percentUsed + '%).'
        : esc(a.category) + ' budget is at ' + a.percentUsed + '%: ' + money(a.remaining) + ' left of ' + money(a.limitAmount) + '.';
      return '<div class="alert ' + (exceeded ? 'exceeded' : 'warning') + '"><span aria-hidden="true">' + (exceeded ? '&#9888;' : '&#9889;') + '</span><span>' + text + '</span></div>';
    }).join('');
  }

  function drawCashflow(points) {
    if (typeof Chart === 'undefined') { return; }
    if (state.charts.cash) { state.charts.cash.destroy(); }
    state.charts.cash = new Chart($('#cashflowChart'), {
      type: 'bar',
      data: {
        labels: points.map((p) => p.label),
        datasets: [
          { label: 'Income', data: points.map((p) => Number(p.income)), backgroundColor: '#10b981', borderRadius: 6 },
          { label: 'Expenses', data: points.map((p) => Number(p.expense)), backgroundColor: '#f43f5e', borderRadius: 6 },
          { type: 'line', label: 'Net', data: points.map((p) => Number(p.net)), borderColor: '#5b5bf0', backgroundColor: '#5b5bf0', tension: 0.35 }
        ]
      },
      options: {
        responsive: true, maintainAspectRatio: false,
        plugins: { legend: { position: 'bottom' }, tooltip: { callbacks: { label: (c) => c.dataset.label + ': ' + money(c.parsed.y) } } },
        scales: { y: { ticks: { callback: (v) => money(v) } } }
      }
    });
  }

  function drawCategories(rows) {
    const empty = $('#categoryEmpty');
    const canvas = $('#categoryChart');
    if (state.charts.cat) { state.charts.cat.destroy(); state.charts.cat = null; }
    empty.hidden = rows.length > 0;
    canvas.hidden = rows.length === 0;
    if (!rows.length || typeof Chart === 'undefined') { return; }
    state.charts.cat = new Chart(canvas, {
      type: 'doughnut',
      data: {
        labels: rows.map((r) => r.category),
        datasets: [{ data: rows.map((r) => Number(r.total)), backgroundColor: rows.map((_, i) => PALETTE[i % PALETTE.length]), borderWidth: 0 }]
      },
      options: {
        responsive: true, maintainAspectRatio: false, cutout: '62%',
        plugins: { legend: { position: 'bottom' }, tooltip: { callbacks: { label: (c) => c.label + ': ' + money(c.parsed) + ' (' + rows[c.dataIndex].percentage + '%)' } } }
      }
    });
  }

  function renderBudgets(budgets) {
    state.budgets = budgets;
    const list = $('#budgetList');
    if (!budgets.length) {
      list.innerHTML = '<p class="empty">No budgets for ' + esc(monthLabel(state.period)) + '. Use "Set budget" to create one.</p>';
      return;
    }
    list.innerHTML = budgets.map((b) => {
      const level = b.alertLevel.toLowerCase();
      const width = Math.min(Number(b.percentUsed), 100);
      const badge = b.alertLevel === 'OK' ? 'On track' : (b.alertLevel === 'WARNING' ? '80%+ used' : 'Over budget');
      return '<div class="budget"><div class="budget-top"><strong>' + esc(b.category) + '</strong><span class="badge ' + (b.alertLevel === 'OK' ? '' : level) + '">' + badge + '</span></div>' +
        '<div class="progress ' + (b.alertLevel === 'OK' ? '' : level) + '" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="' + width + '"><span style="width:' + width + '%"></span></div>' +
        '<div class="budget-meta"><span>' + money(b.spent) + ' of ' + money(b.limitAmount) + '</span><span>' + b.percentUsed + '%</span></div>' +
        '<div class="budget-meta"><span>' + (Number(b.remaining) >= 0 ? money(b.remaining) + ' left' : money(Math.abs(Number(b.remaining))) + ' over') + '</span>' +
        '<span class="row-actions"><button type="button" class="btn btn-ghost btn-sm" data-action="edit-budget" data-id="' + b.id + '">Edit</button>' +
        '<button type="button" class="btn btn-danger btn-sm" data-action="delete-budget" data-id="' + b.id + '">Delete</button></span></div></div>';
    }).join('');
  }

  function renderTransactions(rows) {
    state.transactions = rows;
    $('#txCount').textContent = rows.length + (rows.length === 1 ? ' transaction' : ' transactions');
    $('#txEmpty').hidden = rows.length > 0;
    $('#txBody').innerHTML = rows.map((t) => {
      const income = t.type === 'INCOME';
      return '<tr><td>' + esc(t.transactionDate) + '</td><td>' + esc(t.category) + '</td><td>' + esc(t.description || '') + '</td>' +
        '<td><span class="badge ' + (income ? 'income' : 'expense') + '">' + (income ? 'Income' : 'Expense') + '</span></td>' +
        '<td class="right amount ' + (income ? 'pos' : 'neg') + '">' + (income ? '+' : '-') + money(t.amount) + '</td>' +
        '<td class="right"><span class="row-actions"><button type="button" class="btn btn-ghost btn-sm" data-action="edit-tx" data-id="' + t.id + '">Edit</button>' +
        '<button type="button" class="btn btn-danger btn-sm" data-action="delete-tx" data-id="' + t.id + '">Delete</button></span></td></tr>';
    }).join('');
  }

  // ---------- data loading ----------
  async function loadDashboard() {
    const q = periodQuery();
    $('#catMonthLabel').textContent = monthLabel(state.period);
    const [summary, cash, cats, budgets, alerts] = await Promise.all([
      api('/analytics/summary?' + q),
      api('/analytics/cashflow?months=6&' + q),
      api('/analytics/categories?' + q),
      api('/budgets?' + q),
      api('/alerts?' + q)
    ]);
    renderSummary(summary);
    drawCashflow(cash);
    drawCategories(cats);
    renderBudgets(budgets);
    renderAlerts(alerts);
  }

  async function loadTransactions() {
    const params = new URLSearchParams();
    const add = (key, value) => { if (value) { params.set(key, value); } };
    add('type', $('#fType').value);
    add('category', $('#fCategory').value.trim());
    add('from', $('#fFrom').value);
    add('to', $('#fTo').value);
    add('search', $('#fSearch').value.trim());
    const qs = params.toString();
    renderTransactions(await api('/transactions' + (qs ? '?' + qs : '')));
  }

  async function loadCategories() {
    const cats = await api('/categories');
    $('#categoryOptions').innerHTML = cats.map((c) => '<option value="' + esc(c) + '"></option>').join('');
  }

  async function refresh() {
    try { await Promise.all([loadDashboard(), loadTransactions(), loadCategories()]); }
    catch (e) { toast(e.message, true); }
  }

  // ---------- dialogs ----------
  function showError(id, err) {
    const el = $(id);
    const details = Object.values(err.fieldErrors || {});
    el.textContent = details.length ? details.join(' ') : err.message;
    el.hidden = false;
  }

  function openTxDialog(tx) {
    $('#txError').hidden = true;
    $('#txDialogTitle').textContent = tx ? 'Edit transaction' : 'Add transaction';
    $('#txId').value = tx ? tx.id : '';
    $('#txType').value = tx ? tx.type : 'EXPENSE';
    $('#txAmount').value = tx ? tx.amount : '';
    $('#txCategory').value = tx ? tx.category : '';
    $('#txDate').value = tx ? tx.transactionDate : today();
    $('#txDescription').value = tx && tx.description ? tx.description : '';
    $('#txDialog').showModal();
  }

  function openBudgetDialog(b) {
    $('#budgetError').hidden = true;
    $('#budgetDialogTitle').textContent = b ? 'Edit budget' : 'Set budget';
    $('#budgetId').value = b ? b.id : '';
    $('#budgetCategory').value = b ? b.category : '';
    $('#budgetMonth').value = b ? b.year + '-' + String(b.month).padStart(2, '0') : state.period;
    $('#budgetLimit').value = b ? b.limitAmount : '';
    $('#budgetDialog').showModal();
  }

  async function submitTx(event) {
    event.preventDefault();
    const id = $('#txId').value;
    const payload = {
      type: $('#txType').value,
      amount: $('#txAmount').value,
      category: $('#txCategory').value.trim(),
      description: $('#txDescription').value.trim(),
      transactionDate: $('#txDate').value
    };
    try {
      await api(id ? '/transactions/' + id : '/transactions', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
      $('#txDialog').close();
      toast(id ? 'Transaction updated' : 'Transaction added');
      await refresh();
    } catch (e) { showError('#txError', e); }
  }

  async function submitBudget(event) {
    event.preventDefault();
    const id = $('#budgetId').value;
    const month = $('#budgetMonth').value;
    if (!month) { showError('#budgetError', new Error('Month is required')); return; }
    const p = parts(month);
    const payload = { category: $('#budgetCategory').value.trim(), year: p.year, month: p.month, limitAmount: $('#budgetLimit').value };
    try {
      await api(id ? '/budgets/' + id : '/budgets', { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
      $('#budgetDialog').close();
      toast(id ? 'Budget updated' : 'Budget saved');
      await refresh();
    } catch (e) { showError('#budgetError', e); }
  }

  async function remove(path, label) {
    if (!window.confirm('Delete this ' + label + '? This cannot be undone.')) { return; }
    try { await api(path, { method: 'DELETE' }); toast(label.charAt(0).toUpperCase() + label.slice(1) + ' deleted'); await refresh(); }
    catch (e) { toast(e.message, true); }
  }

  // ---------- wiring ----------
  function setPeriod(value) {
    if (!/^\d{4}-\d{2}$/.test(value)) { return; }
    state.period = value;
    $('#monthPicker').value = value;
    loadDashboard().catch((e) => toast(e.message, true));
  }

  function shiftMonth(delta) {
    const p = parts(state.period);
    setPeriod(toMonthValue(new Date(p.year, p.month - 1 + delta, 1)));
  }

  function init() {
    $('#monthPicker').value = state.period;
    $('#monthPicker').addEventListener('change', (e) => setPeriod(e.target.value));
    $('#prevMonth').addEventListener('click', () => shiftMonth(-1));
    $('#nextMonth').addEventListener('click', () => shiftMonth(1));
    $('#addTxBtn').addEventListener('click', () => openTxDialog(null));
    $('#addBudgetBtn').addEventListener('click', () => openBudgetDialog(null));
    $('#txForm').addEventListener('submit', submitTx);
    $('#budgetForm').addEventListener('submit', submitBudget);
    document.querySelectorAll('[data-close]').forEach((btn) => btn.addEventListener('click', () => $('#' + btn.dataset.close).close()));

    let timer;
    ['#fType', '#fCategory', '#fFrom', '#fTo'].forEach((sel) => $(sel).addEventListener('change', () => loadTransactions().catch((e) => toast(e.message, true))));
    $('#fSearch').addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(() => loadTransactions().catch((e) => toast(e.message, true)), 300); });
    $('#resetFilters').addEventListener('click', () => {
      ['#fType', '#fCategory', '#fFrom', '#fTo', '#fSearch'].forEach((sel) => { $(sel).value = ''; });
      loadTransactions().catch((e) => toast(e.message, true));
    });

    document.addEventListener('click', (e) => {
      const btn = e.target.closest('[data-action]');
      if (!btn) { return; }
      const id = Number(btn.dataset.id);
      switch (btn.dataset.action) {
        case 'edit-tx': openTxDialog(state.transactions.find((t) => t.id === id)); break;
        case 'delete-tx': remove('/transactions/' + id, 'transaction'); break;
        case 'edit-budget': openBudgetDialog(state.budgets.find((b) => b.id === id)); break;
        case 'delete-budget': remove('/budgets/' + id, 'budget'); break;
        default: break;
      }
    });

    api('/config').then((c) => { if (c && c.currency) { state.currency = c.currency; } }).catch(() => { /* keep default currency */ })
      .finally(refresh);
    if (typeof Chart === 'undefined') { toast('Chart library failed to load; charts are unavailable.', true); }
  }

  document.addEventListener('DOMContentLoaded', init);
})();
