// ================================================================
// 中古車在庫管理 — フロントエンド本体 (依存なし・ES Module)
// ================================================================
import { api, ApiError } from './api.js';
import * as F from './format.js';
import * as C from './charts.js';

const state = {
  view: 'dashboard',
  months: 12,
  query: {
    search: '',
    status: 'ALL',
    stockType: '',
    saleType: '',
    stockDateFrom: '',
    stockDateTo: '',
    sort: 'stockDate',
    dir: 'desc',
    page: 0,
    size: 25,
  },
  meta: { statuses: [], stockTypes: [], saleTypes: [], manufacturers: [], suppliers: [], buyers: [], staff: [] },
  page: null,
  dashboard: null,
};

const els = {
  dashboard: document.getElementById('view-dashboard'),
  cars: document.getElementById('view-cars'),
  pageTitle: document.getElementById('page-title'),
  pageSub: document.getElementById('page-sub'),
  search: document.getElementById('global-search'),
  drawer: document.getElementById('drawer'),
  drawerBackdrop: document.getElementById('drawer-backdrop'),
  modalRoot: document.getElementById('modal-root'),
  toasts: document.getElementById('toasts'),
  navCount: document.getElementById('nav-count'),
  sidebarSummary: document.getElementById('sidebar-summary'),
};

const VIEW_META = {
  dashboard: { title: 'ダッシュボード', sub: '在庫状況・粗利推移・アラートを一目で把握' },
  cars: { title: '在庫一覧', sub: '検索・絞り込み・登録 / 編集 / 削除' },
};

// ================================================================
// 起動
// ================================================================
init();

async function init() {
  applyTheme(localStorage.getItem('usedcar.theme') ?? 'dark');

  document.querySelectorAll('[data-nav]').forEach((btn) => {
    btn.addEventListener('click', () => setView(btn.dataset.nav));
  });
  document.getElementById('btn-new').addEventListener('click', () => openCarForm(null));
  document.getElementById('btn-export').addEventListener('click', () => {
    window.location.href = api.exportUrl(state.query);
    toast('info', 'CSV 出力', '現在の検索条件でダウンロードします');
  });
  document.getElementById('theme-toggle').addEventListener('click', toggleTheme);

  let timer = null;
  els.search.addEventListener('input', () => {
    clearTimeout(timer);
    timer = setTimeout(() => {
      state.query.search = els.search.value.trim();
      state.query.page = 0;
      if (state.view !== 'cars') setView('cars');
      else loadCars();
    }, 320);
  });
  els.search.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      clearTimeout(timer);
      state.query.search = els.search.value.trim();
      state.query.page = 0;
      setView('cars');
    }
  });

  document.addEventListener('click', onGlobalClick);
  document.addEventListener('keydown', onGlobalKey);

  try {
    state.meta = { ...state.meta, ...(await api.meta()) };
    fillDatalists();
  } catch (err) {
    console.warn('メタ情報の取得に失敗', err);
  }

  await Promise.all([loadDashboard(), loadCars()]);
}

// ================================================================
// 共通 UI
// ================================================================
function setView(view) {
  state.view = view;
  document.querySelectorAll('[data-nav]').forEach((b) => b.classList.toggle('is-active', b.dataset.nav === view));
  els.dashboard.classList.toggle('is-hidden', view !== 'dashboard');
  els.cars.classList.toggle('is-hidden', view !== 'cars');
  els.pageTitle.textContent = VIEW_META[view].title;
  els.pageSub.textContent = VIEW_META[view].sub;
  if (view === 'dashboard') loadDashboard();
  else loadCars();
}

function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  localStorage.setItem('usedcar.theme', theme);
}
function toggleTheme() {
  applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
}

function toast(kind, title, message = '') {
  const el = document.createElement('div');
  el.className = `toast ${kind}`;
  el.innerHTML = `<div><strong>${F.esc(title)}</strong>${message ? `<span>${F.esc(message)}</span>` : ''}</div>`;
  els.toasts.appendChild(el);
  setTimeout(() => {
    el.classList.add('leaving');
    setTimeout(() => el.remove(), 220);
  }, 3600);
}

function onGlobalKey(e) {
  if (e.key === 'Escape') {
    closeModal();
    closeDrawer();
    return;
  }
  const tag = (e.target.tagName ?? '').toLowerCase();
  if (['input', 'textarea', 'select'].includes(tag)) return;
  if (e.key === '/') {
    e.preventDefault();
    els.search.focus();
  } else if (e.key.toLowerCase() === 'n') {
    e.preventDefault();
    openCarForm(null);
  }
}

function onGlobalClick(e) {
  const nav = e.target.closest('[data-nav]');
  if (nav) return; // 側ナビは個別に登録済み

  const close = e.target.closest('[data-close]');
  if (close) {
    if (close.dataset.close === 'drawer') closeDrawer();
    else closeModal();
    return;
  }

  const action = e.target.closest('[data-action]');
  if (!action) return;
  const { action: name, id } = action.dataset;

  switch (name) {
    case 'status':
      state.query.status = action.dataset.value ?? 'ALL';
      state.query.page = 0;
      loadCars();
      break;
    case 'sort':
      applySort(action.dataset.sort);
      break;
    case 'row':
      openDrawer(Number(id));
      break;
    case 'edit':
      openCarForm(Number(id));
      break;
    case 'delete':
      confirmDelete(Number(id));
      break;
    case 'prev':
      state.query.page = Math.max(0, state.query.page - 1);
      loadCars();
      break;
    case 'next':
      state.query.page = state.query.page + 1;
      loadCars();
      break;
    case 'months':
      state.months = Number(action.dataset.value ?? 12);
      loadDashboard();
      break;
    case 'alert':
      openDrawer(Number(id));
      break;
    case 'reset-filters':
      resetFilters();
      break;
    case 'confirm-delete':
      doDelete(Number(action.dataset.id));
      break;
    case 'save':
      submitForm();
      break;
    default:
      break;
  }
}

function applySort(field) {
  if (state.query.sort === field) {
    state.query.dir = state.query.dir === 'asc' ? 'desc' : 'asc';
  } else {
    state.query.sort = field;
    state.query.dir = ['model', 'manufacturer', 'chassisNumber', 'firstRegistration'].includes(field) ? 'asc' : 'desc';
  }
  state.query.page = 0;
  loadCars();
}

function resetFilters() {
  state.query = { ...state.query, search: '', status: 'ALL', stockType: '', saleType: '', stockDateFrom: '', stockDateTo: '', page: 0 };
  els.search.value = '';
  loadCars();
}

function fillDatalists() {
  const fill = (id, items) => {
    const node = document.getElementById(id);
    if (!node) return;
    node.innerHTML = (items ?? []).map((v) => `<option value="${F.esc(v)}"></option>`).join('');
  };
  fill('dl-manufacturers', state.meta.manufacturers);
  fill('dl-suppliers', state.meta.suppliers);
  fill('dl-buyers', state.meta.buyers);
  fill('dl-staff', state.meta.staff);
}

// ================================================================
// ダッシュボード
// ================================================================
async function loadDashboard() {
  els.dashboard.innerHTML = skeletonBlock();
  try {
    state.dashboard = await api.dashboard(state.months);
    renderDashboard();
    renderSidebarSummary(state.dashboard);
    els.navCount.textContent = state.dashboard.counts.inStock;
  } catch (err) {
    els.dashboard.innerHTML = errorBlock(err);
  }
}

function renderSidebarSummary(d) {
  const row = (label, value) => `<div class="mini-stat"><span>${label}</span><strong>${F.esc(value)}</strong></div>`;
  els.sidebarSummary.innerHTML = [
    row('在庫中', d.counts.inStock),
    row('契約待', d.counts.contracted),
    row('納車済', d.counts.sold),
    `<div class="divider"></div>`,
    row('入庫予定', d.counts.scheduledIncoming),
    row('車検注意', d.counts.inspectionExpiringSoon + d.counts.inspectionOverdue),
  ].join('');
}

function renderDashboard() {
  const d = state.dashboard;
  const inv = d.inventory;
  const rel = d.realized;

  const thisMonthAvg = rel.thisMonthUnits > 0 ? rel.thisMonthGrossProfit / rel.thisMonthUnits : 0;
  const kpi = `
    <div class="grid grid-kpi">
      ${kpiCard('在庫台数', `${F.esc(d.counts.inStock)} <small>台</small>`,
        `契約待 ${d.counts.contracted} 台 / 納車済 ${d.counts.sold} 台 / 登録 ${d.counts.total} 台`, '')}
      ${kpiCard('在庫 原価合計', F.money(inv.totalCost), `平均販売価格 ${plain(F.money(inv.averageSalePrice))}`, '')}
      ${kpiCard('在庫 潜在粗利', plain(F.signedMoney(inv.potentialGrossProfit)),
        `1台あたり平均 ${plain(F.money(inv.averageGrossProfit))}`, inv.potentialGrossProfit >= 0 ? 'is-positive' : 'is-negative')}
      ${kpiCard('実績粗利（累計）', plain(F.signedMoney(rel.grossProfit)),
        `${F.deltaChip(rel.thisMonthGrossProfit, rel.lastMonthGrossProfit)} 粗利率 ${plain(F.pct(rel.grossProfitRate))}`,
        rel.grossProfit >= 0 ? 'is-positive' : 'is-negative')}
      ${kpiCard('今月の実績粗利', plain(F.signedMoney(rel.thisMonthGrossProfit)),
        `成約 ${rel.thisMonthUnits} 台 / 1台平均 ${plain(F.money(thisMonthAvg))}`, 'is-warning')}
      ${kpiCard('平均在庫日数', `${F.esc(inv.averageStockDays)} <small>日</small>`,
        `長期在庫 ${inv.longStockUnits} 台 / 車検注意 ${d.counts.inspectionExpiringSoon + d.counts.inspectionOverdue} 台`,
        inv.longStockUnits > 0 ? 'is-negative' : '')}
    </div>`;

  const monthly = `
    <div class="card">
      <div class="card-head">
        <div>
          <h2>月別 実績粗利</h2>
          <div class="sub">契約日基準で計上（${d.months} ヶ月）</div>
        </div>
        <div class="spacer"></div>
        <div class="group">
          ${[6, 12, 24].map((m) => `<button class="chip ${d.months === m ? 'is-active' : ''}" data-action="months" data-value="${m}">${m}ヶ月</button>`).join('')}
        </div>
      </div>
      <div class="card-body">${C.profitBarChart(d.monthly)}</div>
    </div>`;

  const saleMix = `
    <div class="card">
      <div class="card-head"><h2>販売区分別 実績</h2><div class="sub">台数構成比 / 粗利</div></div>
      <div class="card-body">
        <div class="grid" style="grid-template-columns:minmax(0,200px) minmax(0,1fr);align-items:center">
          ${C.donutChart(d.saleTypes.map((s) => ({ label: s.label, value: s.units })), { centerLabel: '台', subLabel: '成約' })}
          <div class="legend">
            ${C.legendRows(d.saleTypes.map((s) => ({ label: s.label, value: s.units, extra: s.grossProfit })), { unit: '台' })}
          </div>
        </div>
      </div>
    </div>`;

  const stockMix = `
    <div class="card">
      <div class="card-head"><h2>仕入区分別（在庫中）</h2><div class="sub">原価合計</div></div>
      <div class="card-body">${C.progressRows(d.stockTypes.map((s) => ({ label: `${s.label} (${s.units})`, value: s.totalCost })))}</div>
    </div>`;

  const aging = `
    <div class="card">
      <div class="card-head"><h2>在庫日数の分布</h2><div class="sub">入庫日からの経過日数</div></div>
      <div class="card-body">${C.histogram(d.aging)}</div>
    </div>`;

  const makers = `
    <div class="card">
      <div class="card-head"><h2>メーカー別 在庫</h2><div class="sub">台数</div></div>
      <div class="card-body">${d.manufacturers.length
        ? C.progressRows(d.manufacturers.map((m) => ({ label: `${m.name} (${m.units})`, value: m.units, suffix: ' 台' })))
        : emptyBlock('在庫データがありません')}</div>
    </div>`;

  const alerts = `
    <div class="card">
      <div class="card-head">
        <h2>車検アラート</h2><div class="sub">満了 ${30} 日以内 / 車検切れ（在庫中）</div>
      </div>
      <div class="card-body">
        ${d.inspectionAlerts.length ? alertList(d.inspectionAlerts, (c) =>
          c.daysUntilInspection < 0
            ? `<span class="badge flag flag-danger">車検切れ ${Math.abs(c.daysUntilInspection)} 日経過</span>`
            : `<span class="badge flag flag-warn">あと ${c.daysUntilInspection} 日</span>`) : emptyBlock('車検接近の在庫はありません')}
      </div>
    </div>`;

  const longStock = `
    <div class="card">
      <div class="card-head"><h2>長期在庫アラート</h2><div class="sub">90 日超え（在庫中）</div></div>
      <div class="card-body">
        ${d.longStockAlerts.length ? alertList(d.longStockAlerts, (c) =>
          `<span class="badge flag flag-danger">${c.stockDays} 日</span>`) : emptyBlock('長期在庫はありません')}
      </div>
    </div>`;

  els.dashboard.innerHTML = `
    ${kpi}
    <div class="section grid grid-2">${monthly}${saleMix}</div>
    <div class="section grid grid-3">${stockMix}${aging}${makers}</div>
    <div class="section grid grid-2">${alerts}${longStock}</div>
  `;
}

function kpiCard(label, value, foot, modifier = '') {
  return `
    <div class="kpi ${modifier}">
      <div class="kpi-label">${F.esc(label)}</div>
      <div class="kpi-value">${value}</div>
      <div class="kpi-foot"><span class="kpi-note">${foot}</span></div>
    </div>`;
}

function plain(html) {
  const tmp = document.createElement('div');
  tmp.innerHTML = html;
  return tmp.textContent;
}

function alertList(items, badge) {
  return `<div class="alert-list">` + items.map((c) => `
    <div class="alert-item" data-action="alert" data-id="${c.id}">
      <div>
        <div class="name">${F.esc(c.manufacturer)} ${F.esc(c.model)}</div>
        <div class="meta">車台 ${F.esc(c.chassisNumber)} ・ 入庫 ${c.stockDate ?? '予定'} ・ 車検 ${c.inspectionExpiry ?? '—'}</div>
      </div>
      ${badge(c)}
    </div>`).join('') + `</div>`;
}

// ================================================================
// 在庫一覧
// ================================================================
async function loadCars() {
  els.cars.innerHTML = filterBar() + `<div class="card" style="margin-top:14px"><div class="card-body"><div class="skeleton card-lg"></div></div></div>`;
  try {
    state.page = await api.listCars(state.query);
    renderCars();
  } catch (err) {
    els.cars.innerHTML = filterBar() + errorBlock(err);
  }
}

const COLUMNS = [
  { key: 'model', label: '車両', sort: 'model', hide: false },
  { key: 'chassisNumber', label: '車台番号', sort: 'chassisNumber' },
  { key: 'basic', label: '初度 / 走行', sort: 'firstRegistration' },
  { key: 'inspection', label: '車検満了日', sort: 'inspectionExpiry' },
  { key: 'stock', label: '入庫 / 在庫日数', sort: 'stockDays' },
  { key: 'salePrice', label: '販売価格', sort: 'salePrice', right: true },
  { key: 'cost', label: '原価', sort: 'totalCost', right: true },
  { key: 'profit', label: '粗利', sort: 'grossProfit', right: true },
];

function sortIndicator(field) {
  if (state.query.sort !== field) return '';
  return `<span class="sort-ind">${state.query.dir === 'asc' ? '▲' : '▼'}</span>`;
}

function renderCars() {
  const p = state.page;
  const head = `
    <tr>
      <th>状態</th>
      ${COLUMNS.map((col) => `<th class="sortable ${col.right ? 'right' : ''}" data-action="sort" data-sort="${col.sort}">${col.label}${sortIndicator(col.sort)}</th>`).join('')}
      <th class="right"></th>
    </tr>`;

  const rows = p.content.map((c) => `
    <tr data-action="row" data-id="${c.id}">
      <td>${F.statusPill(c.status, c.statusLabel)}${c.scheduledIncoming ? ' <span class="tag">入庫予定</span>' : ''}</td>
      <td>
        <div class="cell-strong">${F.esc(c.manufacturer)} ${F.esc(c.model)}</div>
        <div class="cell-sub">${F.esc(c.vehicleType ?? '')}${c.saleTypeLabel ? ` ・ ${F.esc(c.saleTypeLabel)}` : ''}</div>
      </td>
      <td class="num">${F.esc(c.chassisNumber)}</td>
      <td>
        ${F.monthText(c.firstRegistration)}
        <div class="cell-sub">${F.num(c.mileage, ' km')}</div>
      </td>
      <td>${F.dateText(c.inspectionExpiry)}<div>${F.inspectionFlag(c.daysUntilInspection)}</div></td>
      <td>
        ${F.dateText(c.stockDate || c.scheduledStockDate)}
        <div class="cell-sub">${F.daysText(c.stockDays)} ${F.ageFlag(c.stockDays ?? -1)}</div>
      </td>
      <td class="right">${F.money(c.salePrice)}</td>
      <td class="right">${F.money(c.totalCost)}</td>
      <td class="right">
        <div>${F.signedMoney(c.grossProfit)}</div>
        <div class="cell-sub">${F.pct(c.grossProfitRate)}</div>
      </td>
      <td class="right">
        <div class="cell-actions">
          <button class="mini-btn" data-action="edit" data-id="${c.id}">編集</button>
          <button class="mini-btn danger" data-action="delete" data-id="${c.id}">削除</button>
        </div>
      </td>
    </tr>`).join('');

  els.cars.innerHTML = `
    ${filterBar()}
    <div class="card" style="margin-top:14px">
      <div class="table-wrap">
        <table class="data">
          <thead>${head}</thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
      ${p.content.length ? '' : emptyBlock('条件に一致する在庫がありません。検索条件をリセットするか、新規登録してください。')}
      <div class="table-foot">
        <span>全 <strong class="page-ind">${p.totalElements}</strong> 件 ／ ${p.totalPages} ページ中 ${p.page + 1} ページ目</span>
        <span>表示件数
          <select id="page-size">
            ${[10, 25, 50, 100].map((n) => `<option value="${n}" ${n === p.size ? 'selected' : ''}>${n}</option>`).join('')}
          </select>
        </span>
        <div class="pager">
          <button class="mini-btn" data-action="prev" ${p.first ? 'disabled' : ''}>← 前へ</button>
          <span class="page-ind">${p.page + 1} / ${Math.max(1, p.totalPages)}</span>
          <button class="mini-btn" data-action="next" ${p.last ? 'disabled' : ''}>次へ →</button>
        </div>
      </div>
    </div>`;

  const sizeSelect = document.getElementById('page-size');
  if (sizeSelect) {
    sizeSelect.addEventListener('change', () => {
      state.query.size = Number(sizeSelect.value);
      state.query.page = 0;
      loadCars();
    });
  }
}

function filterBar() {
  const q = state.query;
  const chips = state.meta.statuses.map((s) =>
    `<button class="chip ${q.status === s.value ? 'is-active' : ''}" data-action="status" data-value="${s.value}">${F.esc(s.label)}</button>`).join('');

  const options = (items, selected) => items
    .map((i) => `<option value="${i.value}" ${selected === i.value ? 'selected' : ''}>${F.esc(i.label)}</option>`).join('');

  return `
    <div class="card">
      <div class="filters">
        <div class="group"><span class="group-label">状態</span>${chips}</div>
        <div class="group">
          <span class="group-label">入庫区分</span>
          <select data-filter="stockType"><option value="">すべて</option>${options(state.meta.stockTypes, q.stockType)}</select>
        </div>
        <div class="group">
          <span class="group-label">販売区分</span>
          <select data-filter="saleType"><option value="">すべて</option>${options(state.meta.saleTypes, q.saleType)}</select>
        </div>
        <div class="group">
          <span class="group-label">入庫日</span>
          <span class="field-inline">
            <input type="date" data-filter="stockDateFrom" value="${q.stockDateFrom}">
            <span class="dim">〜</span>
            <input type="date" data-filter="stockDateTo" value="${q.stockDateTo}">
          </span>
        </div>
        <div class="group" style="margin-left:auto">
          <button class="btn btn-sm btn-ghost" data-action="reset-filters">条件リセット</button>
        </div>
      </div>
    </div>`;
}

function skeletonBlock() {
  return `<div class="grid grid-kpi">${'<div class="skeleton" style="height:96px"></div>'.repeat(6)}</div>
    <div class="section grid grid-2"><div class="skeleton card-lg"></div><div class="skeleton card-lg"></div></div>`;
}

function emptyBlock(text) {
  return `<div class="empty">
    <svg viewBox="0 0 24 24"><path d="M4 7h16M4 12h10M4 17h7" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="round"/></svg>
    <h3>${F.esc(text)}</h3>
  </div>`;
}

function errorBlock(err) {
  return `<div class="card"><div class="card-body">
    <div class="form-banner">データ取得に失敗しました: ${F.esc(err.message ?? err)}</div>
  </div></div>`;
}

// ================================================================
// 詳細ドロワー
// ================================================================
async function openDrawer(id) {
  try {
    const c = await api.getCar(id);
    els.drawer.innerHTML = drawerHtml(c);
    els.drawer.hidden = false;
    els.drawerBackdrop.hidden = false;
  } catch (err) {
    toast('error', '詳細を取得できません', err.message);
  }
}

function closeDrawer() {
  if (els.drawer.hidden) return;
  els.drawer.hidden = true;
  els.drawerBackdrop.hidden = true;
}

function drawerHtml(c) {
  const dl = (rows) => `<dl class="detail-grid">` + rows
    .map(([k, v]) => `<dt>${F.esc(k)}</dt><dd>${v}</dd>`).join('') + `</dl>`;

  const timeline = [
    ['入庫予定', c.scheduledStockDate],
    ['入庫', c.stockDate],
    ['契約', c.contractDate],
    ['名義変更', c.transferDate],
    ['納車', c.deliveryDate],
  ].map(([label, date]) => `
    <div class="timeline-item ${date ? 'done' : 'pending'}">
      <span class="timeline-dot"></span>
      <div>
        <div class="timeline-label">${label}</div>
        <div class="timeline-value">${date ? F.dateText(date) : '未実施'}</div>
      </div>
    </div>`).join('');

  const profit = `
    <div class="profit-box">
      <div class="profit-line"><span>車両本体</span><span class="val">${F.money(c.baseCost)}</span></div>
      <div class="profit-line"><span>リサイクル預託金</span><span class="val">${F.money(c.recycleFee)}</span></div>
      <div class="profit-line"><span>輸送費</span><span class="val">${F.money(c.transportFee)}</span></div>
      <div class="profit-line"><span>加修費</span><span class="val">${F.money(c.repairFee)}</span></div>
      <div class="profit-line profit-total"><span>原価合計</span><span class="val">${F.money(c.totalCost)}</span></div>
      <div class="profit-line"><span>販売価格</span><span class="val">${F.money(c.salePrice)}</span></div>
      <div class="profit-line profit-total"><span>粗利（販売価格 − 原価）</span><span class="val">${F.signedMoney(c.grossProfit)} ${F.pct(c.grossProfitRate)}</span></div>
    </div>`;

  return `
    <div class="drawer-head">
      <div>
        <h2>${F.esc(c.manufacturer)} ${F.esc(c.model)}</h2>
        <div class="sub">車台番号 ${F.esc(c.chassisNumber)}${c.vehicleType ? ` ・ ${F.esc(c.vehicleType)}` : ''}</div>
        <div style="margin-top:8px;display:flex;gap:6px;flex-wrap:wrap">
          ${F.statusPill(c.status, c.statusLabel)}
          ${c.stockTypeLabel ? `<span class="pill">${F.esc(c.stockTypeLabel)}</span>` : ''}
          ${c.saleTypeLabel ? `<span class="pill">${F.esc(c.saleTypeLabel)}</span>` : ''}
        </div>
      </div>
      <button class="close-btn" data-close="drawer" aria-label="閉じる">✕</button>
    </div>
    <div class="drawer-body">
      <div class="detail-group"><h3>粗利計算</h3>${profit}</div>
      <div class="detail-group"><h3>ステータス</h3>${profitStatus(c)}</div>
      <div class="detail-group"><h3>車両の基本情報</h3>${dl([
        ['メーカー', F.esc(c.manufacturer)],
        ['車種', F.esc(c.model)],
        ['車台番号', `<span class="num">${F.esc(c.chassisNumber)}</span>`],
        ['型式', F.esc(c.vehicleType ?? '')],
        ['初度登録年月', F.monthText(c.firstRegistration)],
        ['車検満了日', `${F.dateText(c.inspectionExpiry)} ${F.inspectionFlag(c.daysUntilInspection)}`],
        ['走行距離', F.num(c.mileage, ' km')],
      ])}</div>
      <div class="detail-group"><h3>入庫情報</h3>${dl([
        ['入庫予定日', F.dateText(c.scheduledStockDate)],
        ['入庫日', F.dateText(c.stockDate)],
        ['在庫日数', F.daysText(c.stockDays)],
        ['入庫区分', F.esc(c.stockTypeLabel ?? '')],
        ['仕入先', F.esc(c.supplierName ?? '')],
        ['仕入担当者', F.esc(c.buyerStaff ?? '')],
      ])}</div>
      <div class="detail-group"><h3>販売情報</h3>${dl([
        ['販売区分', F.esc(c.saleTypeLabel ?? '')],
        ['販売先', F.esc(c.buyerName ?? '')],
        ['販売担当者', F.esc(c.sellerStaff ?? '')],
      ])}</div>
      <div class="detail-group"><h3>プロセス</h3><div class="timeline">${timeline}</div></div>
      ${c.memo ? `<div class="detail-group"><h3>備考</h3><p class="muted">${F.esc(c.memo)}</p></div>` : ''}
      <div class="detail-group"><h3>最終更新</h3><p class="dim num">${F.esc(c.updatedAt ?? '')}</p></div>
    </div>
    <div class="drawer-foot">
      <button class="btn btn-primary btn-sm" data-action="edit" data-id="${c.id}">この車両を編集</button>
      <span class="spacer" style="margin-left:auto"></span>
      <button class="btn btn-danger btn-sm" data-action="delete" data-id="${c.id}">削除</button>
    </div>`;
}

function profitStatus(c) {
  const chips = [];
  chips.push(F.statusPill(c.status, c.statusLabel));
  if (c.stockDays != null && c.stockDays >= 0 && c.status === 'IN_STOCK') {
    chips.push(`<span class="pill">在庫 ${c.stockDays} 日</span>`);
  }
  if (c.daysUntilInspection != null && c.daysUntilInspection <= 30) {
    chips.push(F.inspectionFlag(c.daysUntilInspection));
  }
  return `<div style="display:flex;gap:6px;flex-wrap:wrap">${chips.join('')}</div>`;
}

// ================================================================
// 登録 / 編集フォーム
// ================================================================
const FORM_GROUPS = [
  {
    legend: '車両の基本情報',
    fields: [
      { name: 'manufacturer', label: 'メーカー名', type: 'text', required: true, list: 'dl-manufacturers' },
      { name: 'model', label: '車種', type: 'text', required: true },
      { name: 'chassisNumber', label: '車台番号', type: 'text', required: true, hint: '3〜40文字の英数字（大文字変換・重複不可）' },
      { name: 'vehicleType', label: '型式', type: 'text' },
      { name: 'firstRegistration', label: '初度登録年月', type: 'month', required: true },
      { name: 'inspectionExpiry', label: '車検満了日', type: 'date' },
      { name: 'mileage', label: '走行距離 (km)', type: 'number', required: true, min: 0, step: 1 },
    ],
  },
  {
    legend: '入庫情報',
    fields: [
      { name: 'scheduledStockDate', label: '入庫予定日', type: 'date' },
      { name: 'stockDate', label: '入庫日', type: 'date', hint: '未入庫の場合は空欄' },
      { name: 'stockType', label: '入庫区分', type: 'select', optionsKey: 'stockTypes' },
      { name: 'supplierName', label: '仕入先名称', type: 'text', list: 'dl-suppliers' },
      { name: 'buyerStaff', label: '仕入担当者', type: 'text', list: 'dl-staff' },
    ],
  },
  {
    legend: '販売情報',
    fields: [
      { name: 'contractDate', label: '契約日', type: 'date' },
      { name: 'transferDate', label: '名義変更日', type: 'date' },
      { name: 'deliveryDate', label: '納車日', type: 'date' },
      { name: 'saleType', label: '販売区分', type: 'select', optionsKey: 'saleTypes' },
      { name: 'buyerName', label: '販売先名称', type: 'text', list: 'dl-buyers' },
      { name: 'sellerStaff', label: '販売担当者', type: 'text', list: 'dl-staff' },
    ],
  },
  {
    legend: '価格情報',
    fields: [
      { name: 'baseCost', label: '車両本体 (原価)', type: 'money', required: true },
      { name: 'recycleFee', label: 'リサイクル預託金', type: 'money' },
      { name: 'transportFee', label: '輸送費', type: 'money' },
      { name: 'repairFee', label: '加修費', type: 'money' },
      { name: 'salePrice', label: '販売価格', type: 'money' },
      { name: 'memo', label: '備考', type: 'textarea', full: true },
    ],
  },
];

async function openCarForm(id) {
  let car = null;
  if (id) {
    try {
      car = await api.getCar(id);
    } catch (err) {
      toast('error', '既存データを取得できません', err.message);
      return;
    }
  }
  els.modalRoot.hidden = false;
  els.modalRoot.innerHTML = formModal(car);
  wireForm();
  updateCalc();
  const first = els.modalRoot.querySelector('input, select, textarea');
  if (first) first.focus();
}

function closeModal() {
  if (els.modalRoot.hidden) return;
  els.modalRoot.hidden = true;
  els.modalRoot.innerHTML = '';
}

function formModal(car) {
  const body = FORM_GROUPS.map((group) => `
    <fieldset class="group">
      <legend>${F.esc(group.legend)}</legend>
      <div class="field-grid">
        ${group.fields.map((f) => fieldHtml(f, car)).join('')}
      </div>
    </fieldset>`).join('');

  return `
    <div class="modal" role="dialog" aria-modal="true">
      <div class="modal-head">
        <div>
          <h2>${car ? '在庫データを編集' : '新規在庫の登録'}</h2>
          <div class="sub">${car ? `ID ${car.id} ・ 車台 ${F.esc(car.chassisNumber)}` : '車台番号は英数字 3〜40 文字・重複不可'}</div>
        </div>
        <button class="close-btn" data-close="modal" aria-label="閉じる">✕</button>
      </div>
      <div class="modal-body">
        <div id="form-banner"></div>
        ${body}
      </div>
      <div class="modal-foot">
        <div class="calc" id="calc">
          <div class="calc-item"><div class="k">原価合計</div><div class="v" data-calc="cost">¥0</div></div>
          <div class="calc-item"><div class="k">販売価格</div><div class="v" data-calc="price">—</div></div>
          <div class="calc-item is-profit"><div class="k">粗利</div><div class="v" data-calc="profit">—</div></div>
          <div class="calc-item"><div class="k">粗利率</div><div class="v" data-calc="rate">—</div></div>
        </div>
        <div class="spacer"></div>
        <button class="btn btn-ghost" data-close="modal">キャンセル</button>
        <button class="btn btn-primary" data-action="save" ${car ? `data-id="${car.id}"` : ''}>${car ? '保存する' : '登録する'}</button>
      </div>
    </div>`;
}

function fieldHtml(f, car) {
  const value = car ? F.valueOrEmpty(car[f.name]) : '';
  const cls = `field ${f.full ? 'span-2' : ''}`;
  const required = f.required ? '<span class="req">必須</span>' : '';
  const hint = f.hint ? `<span class="hint">${F.esc(f.hint)}</span>` : '';

  let control;
  if (f.type === 'select') {
    const options = (state.meta[f.optionsKey] ?? []);
    control = `<select name="${f.name}" data-field="${f.name}">
      <option value="">指定なし</option>
      ${options.map((o) => `<option value="${o.value}" ${value === o.value ? 'selected' : ''}>${F.esc(o.label)}</option>`).join('')}
    </select>`;
  } else if (f.type === 'textarea') {
    control = `<textarea name="${f.name}" data-field="${f.name}" rows="2" placeholder="メモ・仕入特記・状態など">${F.esc(value)}</textarea>`;
  } else if (f.type === 'money') {
    control = `<div class="money-input"><input type="number" name="${f.name}" data-field="${f.name}"
      min="0" step="1000" inputmode="numeric" value="${F.esc(value)}" placeholder="0"></div>`;
  } else {
    const list = f.list ? `list="${f.list}"` : '';
    const extra = f.type === 'number' ? `min="${f.min ?? 0}" step="${f.step ?? 1}"` : '';
    control = `<input type="${f.type}" name="${f.name}" data-field="${f.name}" ${list} ${extra} value="${F.esc(value)}">`;
  }

  return `
    <div class="${cls}" data-wrap="${f.name}">
      <label for="f-${f.name}">${F.esc(f.label)}${required}</label>
      ${control}
      ${hint}
      <span class="err" data-err="${f.name}"></span>
    </div>`;
}

function wireForm() {
  els.modalRoot.querySelectorAll('[data-field]').forEach((el) => {
    el.addEventListener('input', () => {
      const wrap = el.closest('[data-wrap]');
      wrap?.classList.remove('has-error');
      const err = els.modalRoot.querySelector(`[data-err="${el.dataset.field}"]`);
      if (err) err.textContent = '';
      if (['baseCost', 'recycleFee', 'transportFee', 'repairFee', 'salePrice'].includes(el.dataset.field)) {
        updateCalc();
      }
    });
  });
}

function readForm() {
  const payload = {};
  FORM_GROUPS.forEach((g) => g.fields.forEach((f) => {
    const el = els.modalRoot.querySelector(`[data-field="${f.name}"]`);
    if (!el) return;
    const raw = typeof el.value === 'string' ? el.value.trim() : el.value;
    if (raw === '' || raw === null) {
      payload[f.name] = null;
      return;
    }
    if (f.type === 'number' || f.type === 'money') {
      const n = Number(raw);
      payload[f.name] = Number.isNaN(n) ? null : n;
    } else {
      payload[f.name] = raw;
    }
  }));
  return payload;
}

function updateCalc() {
  const v = readForm();
  const cost = ['baseCost', 'recycleFee', 'transportFee', 'repairFee']
    .reduce((sum, k) => sum + Number(v[k] ?? 0), 0);
  const price = v.salePrice === null ? null : Number(v.salePrice);
  const profit = price === null ? null : price - cost;
  const rate = price && price !== 0 && profit !== null ? (profit / price) * 100 : null;

  const set = (key, text, cls = '') => {
    const el = els.modalRoot.querySelector(`[data-calc="${key}"]`);
    if (!el) return;
    el.textContent = text;
    el.className = cls;
  };
  set('cost', new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY', maximumFractionDigits: 0 }).format(cost));
  set('price', price === null ? '—' : new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY', maximumFractionDigits: 0 }).format(price));
  if (profit === null) {
    set('profit', '—');
    set('rate', '—');
  } else {
    set('profit', `${profit >= 0 ? '+' : ''}${new Intl.NumberFormat('ja-JP', { maximumFractionDigits: 0 }).format(profit)}`, profit >= 0 ? 'v pos' : 'v neg');
    set('rate', rate === null ? '—' : `${rate.toFixed(1)}%`, rate >= 0 ? 'v pos' : 'v neg');
  }
}

async function submitForm() {
  const payload = readForm();
  const editing = els.modalRoot.querySelector('[data-action="save"]')?.dataset.id
    ? Number(els.modalRoot.querySelector('[data-action="save"]').dataset.id)
    : null;

  const btn = els.modalRoot.querySelector('[data-action="save"]');
  if (btn) btn.disabled = true;

  try {
    const saved = editing ? await api.updateCar(editing, payload) : await api.createCar(payload);
    closeModal();
    toast('success', editing ? '更新しました' : '登録しました', `${saved.manufacturer} ${saved.model}（車台 ${saved.chassisNumber}）`);
    await Promise.all([loadCars(), loadDashboard()]);
  } catch (err) {
    showFormErrors(err);
  } finally {
    if (btn) btn.disabled = false;
  }
}

function showFormErrors(err) {
  const banner = document.getElementById('form-banner');
  if (err instanceof ApiError && Object.keys(err.fieldErrors ?? {}).length) {
    if (banner) banner.innerHTML = `<div class="form-banner">${F.esc(err.payload.message ?? '入力内容を確認してください')}</div>`;
    Object.entries(err.fieldErrors).forEach(([field, message]) => {
      const wrap = els.modalRoot.querySelector(`[data-wrap="${field}"]`);
      const errEl = els.modalRoot.querySelector(`[data-err="${field}"]`);
      if (wrap) wrap.classList.add('has-error');
      if (errEl) errEl.textContent = message;
    });
    const firstError = els.modalRoot.querySelector('.has-error input, .has-error select, .has-error textarea');
    firstError?.focus();
  } else {
    if (banner) banner.innerHTML = `<div class="form-banner">保存に失敗しました: ${F.esc(err.message ?? err)}</div>`;
  }
}

// ================================================================
// 削除
// ================================================================
function confirmDelete(id) {
  api.getCar(id).then((c) => {
    els.modalRoot.hidden = false;
    els.modalRoot.innerHTML = `
      <div class="modal" style="max-width:520px" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div><h2>在庫データを削除</h2><div class="sub">この操作は取り消せません</div></div>
          <button class="close-btn" data-close="modal" aria-label="閉じる">✕</button>
        </div>
        <div class="modal-body">
          <p><strong>${F.esc(c.manufacturer)} ${F.esc(c.model)}</strong></p>
          <p class="muted num">車台番号 ${F.esc(c.chassisNumber)} ・ 販売価格 ${F.money(c.salePrice)} ・ 粗利 ${F.signedMoney(c.grossProfit)}</p>
        </div>
        <div class="modal-foot">
          <span class="spacer"></span>
          <button class="btn btn-ghost" data-close="modal">キャンセル</button>
          <button class="btn btn-danger" data-action="confirm-delete" data-id="${c.id}">削除する</button>
        </div>
      </div>`;
  }).catch((err) => toast('error', '削除できません', err.message));
}

async function doDelete(id) {
  try {
    await api.deleteCar(id);
    closeModal();
    closeDrawer();
    toast('success', '削除しました');
    await Promise.all([loadCars(), loadDashboard()]);
  } catch (err) {
    toast('error', '削除に失敗しました', err.message);
  }
}

// ================================================================
// フィルタ入力（一覧再生成後に再接続）
// ================================================================
const observer = new MutationObserver(() => {
  els.cars.querySelectorAll('[data-filter]').forEach((el) => {
    if (el.dataset.wired === '1') return;
    el.dataset.wired = '1';
    el.addEventListener('change', () => {
      state.query[el.dataset.filter] = el.value;
      state.query.page = 0;
      loadCars();
    });
  });
});
observer.observe(els.cars, { childList: true, subtree: true });
