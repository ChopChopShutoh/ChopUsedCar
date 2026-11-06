// 表示用フォーマット集
const moneyFmt = new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY', maximumFractionDigits: 0 });
const numberFmt = new Intl.NumberFormat('ja-JP');

export function esc(value) {
  if (value === null || value === undefined) return '';
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function money(value) {
  if (value === null || value === undefined || value === '') return '<span class="dim">—</span>';
  return moneyFmt.format(Number(value));
}

export function signedMoney(value) {
  if (value === null || value === undefined || value === '') return '<span class="dim">—</span>';
  const n = Number(value);
  const cls = n > 0 ? 'pos' : n < 0 ? 'neg' : 'muted';
  return `<span class="${cls} num">${money(n)}</span>`;
}

export function num(value, suffix = '') {
  if (value === null || value === undefined || value === '') return '<span class="dim">—</span>';
  return `<span class="num">${numberFmt.format(Number(value))}${suffix}</span>`;
}

export function pct(value, digits = 1) {
  if (value === null || value === undefined || value === '') return '<span class="dim">—</span>';
  const n = Number(value);
  const cls = n > 0 ? 'pos' : n < 0 ? 'neg' : 'muted';
  return `<span class="${cls} num">${n.toFixed(digits)}%</span>`;
}

export function dateText(iso) {
  if (!iso) return '<span class="dim">—</span>';
  return `<span class="num">${iso.replaceAll('-', '/')}</span>`;
}

export function monthText(ym) {
  if (!ym) return '<span class="dim">—</span>';
  const [y, m] = String(ym).split('-');
  return `<span class="num">${y}/${m}</span>`;
}

export function daysText(days) {
  if (days === null || days === undefined) return '<span class="dim">—</span>';
  const d = Number(days);
  if (d < 0) return `<span class="num dim">予定 ${Math.abs(d)} 日後</span>`;
  return `<span class="num">${numberFmt.format(d)} 日</span>`;
}

/** 車検ステータス: 車検切れ / 接近 / 良好 */
export function inspectionFlag(days) {
  if (days === null || days === undefined) return '';
  if (days < 0) return '<span class="flag flag-danger">車検切れ</span>';
  if (days <= 30) return `<span class="flag flag-warn">車検 ${days} 日残</span>`;
  return '';
}

export function ageFlag(days) {
  if (days === null || days === undefined) return '';
  if (days > 180) return '<span class="flag flag-danger">超長期</span>';
  if (days > 90) return '<span class="flag flag-danger">長期在庫</span>';
  if (days > 60) return '<span class="flag flag-warn">要注意</span>';
  return '';
}

export function statusPill(status, label) {
  return `<span class="pill pill-${String(status).toLowerCase()}">${esc(label ?? status)}</span>`;
}

export function deltaChip(current, previous) {
  const c = Number(current ?? 0);
  const p = Number(previous ?? 0);
  const diff = c - p;
  if (diff === 0) return `<span class="delta delta-flat">前月比 ±0</span>`;
  const cls = diff > 0 ? 'delta-up' : 'delta-down';
  const sign = diff > 0 ? '▲' : '▼';
  return `<span class="delta ${cls}">${sign} 前月比 ${moneyFmt.format(Math.abs(diff))}</span>`;
}

export function todayISO() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

export function valueOrEmpty(v) {
  return v === null || v === undefined ? '' : String(v);
}
