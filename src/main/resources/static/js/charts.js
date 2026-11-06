// SVG / DOM チャート生成（依存ライブラリなし）
const PALETTE = ['#6366f1', '#22d3ee', '#34d399', '#fbbf24', '#f87171', '#a78bfa', '#60a5fa', '#f472b6'];

export function compactMoney(value) {
  const n = Number(value ?? 0);
  const abs = Math.abs(n);
  const sign = n < 0 ? '-' : '';
  if (abs >= 10_000_000) return `${sign}${(abs / 10_000_000).toFixed(1)}千万`;
  if (abs >= 10_000) return `${sign}${(abs / 10_000).toFixed(abs >= 1_000_000 ? 0 : 1)}万`;
  return `${sign}${Math.round(abs)}`;
}

/** 月別粗利の棒グラフ（マイナス値にも対応） */
export function profitBarChart(points) {
  const count = Math.max(points.length, 1);
  const colW = Math.max(34, Math.min(64, 760 / count));
  const width = count * colW + 56;
  const height = 240;
  const pad = { top: 18, right: 8, bottom: 30, left: 52 };
  const plotH = height - pad.top - pad.bottom;

  const values = points.map((p) => Number(p.grossProfit ?? 0));
  const maxPos = Math.max(0, ...values);
  const minNeg = Math.min(0, ...values);
  const span = (maxPos - minNeg) || 1;

  const zeroY = pad.top + (maxPos / span) * plotH;
  const scale = (v) => (v / span) * plotH;

  let svg = `<svg class="chart" viewBox="0 0 ${width} ${height}" role="img" aria-label="月別粗利推移">`;
  svg += `<defs><linearGradient id="barGrad" x1="0" y1="0" x2="0" y2="1">
    <stop offset="0%" stop-color="#818cf8"/><stop offset="100%" stop-color="#6366f1"/>
  </linearGradient></defs>`;

  // グリッド + 目盛り
  for (let i = 0; i <= 3; i++) {
    const v = maxPos - (span * i) / 3;
    const y = zeroY - scale(v);
    svg += `<line class="grid-line" x1="${pad.left - 6}" y1="${y.toFixed(1)}" x2="${width - pad.right}" y2="${y.toFixed(1)}"/>`;
    svg += `<text class="axis-label" x="${pad.left - 10}" y="${(y + 3.5).toFixed(1)}" text-anchor="end">${compactMoney(v)}</text>`;
  }

  points.forEach((p, i) => {
    const value = Number(p.grossProfit ?? 0);
    const x = pad.left + i * colW + colW * 0.16;
    const w = colW * 0.68;
    const h = Math.max(2, Math.abs(scale(value)));
    const y = value >= 0 ? zeroY - h : zeroY;
    const label = compactMoney(value);
    const labelY = value >= 0 ? y - 6 : y + h + 12;

    svg += `<g>
      <rect class="${value >= 0 ? 'bar' : 'bar-neg'}" x="${x.toFixed(1)}" y="${y.toFixed(1)}" width="${w.toFixed(1)}"
            height="${h.toFixed(1)}" rx="4" opacity="${value === 0 ? 0.28 : 1}">
        <title>${esc(p.month)} 粗利 ${label} / 成約 ${p.units} 台</title>
      </rect>
      <text class="bar-label" x="${(x + w / 2).toFixed(1)}" y="${labelY.toFixed(1)}">${label}</text>
      <text class="axis-label" x="${(x + w / 2).toFixed(1)}" y="${height - 8}" text-anchor="middle">${esc(p.label)}</text>
    </g>`;
  });

  svg += `<line class="zero-line" x1="${pad.left - 6}" y1="${zeroY.toFixed(1)}" x2="${width - pad.right}" y2="${zeroY.toFixed(1)}"/>`;
  svg += `</svg>`;
  return svg;
}

/** 台数構成比のドーナツ（販売区分別など） */
export function donutChart(items, { centerLabel = '台', subLabel = '合計' } = {}) {
  const size = 190;
  const r = 70;
  const cx = size / 2;
  const cy = size / 2;
  const circumference = 2 * Math.PI * r;
  const values = items.map((i) => Math.max(0, Number(i.value ?? 0)));
  const total = values.reduce((a, b) => a + b, 0);

  let svg = `<svg class="chart" viewBox="0 0 ${size} ${size}" role="img" aria-label="構成比">`;
  svg += `<circle cx="${cx}" cy="${cy}" r="${r}" class="donut-seg" stroke="var(--surface-3)"/>`;

  let offset = 0;
  items.forEach((item, i) => {
    const value = values[i];
    if (value <= 0) return;
    const len = (value / total) * circumference;
    svg += `<circle class="donut-seg" cx="${cx}" cy="${cy}" r="${r}"
      stroke="${PALETTE[i % PALETTE.length]}"
      stroke-dasharray="${len.toFixed(2)} ${(circumference - len).toFixed(2)}"
      stroke-dashoffset="${(-offset).toFixed(2)}"
      transform="rotate(-90 ${cx} ${cy})" stroke-linecap="butt">
      <title>${esc(item.label)}: ${value} ${centerLabel}</title>
    </circle>`;
    offset += len;
  });

  svg += `<text class="donut-center" x="${cx}" y="${cy + 2}">${total}</text>`;
  svg += `<text class="donut-center-sub" x="${cx}" y="${cy + 20}">${esc(subLabel)}（${esc(centerLabel)}）</text>`;
  svg += `</svg>`;
  return svg;
}

export function legendRows(items, { unit = '台', showValue = true, moneyFormat = compactMoney } = {}) {
  return items.map((item, i) => `
    <div class="legend-row">
      <span class="legend-swatch" style="background:${PALETTE[i % PALETTE.length]}"></span>
      <span>${esc(item.label)}</span>
      <span class="units">${item.value} ${unit}</span>
      ${showValue ? `<span class="val">${esc(moneyFormat(item.extra))}</span>` : '<span></span>'}
    </div>`).join('');
}

/** 横棒リスト（メーカー別・仕入区分別など） */
export function progressRows(items) {
  const max = Math.max(1, ...items.map((i) => Math.abs(Number(i.value ?? 0))));
  return `<div class="bars">` + items.map((item, i) => {
    const value = Number(item.value ?? 0);
    const width = (Math.abs(value) / max) * 100;
    return `
      <div class="bar-row">
        <span class="muted">${esc(item.label)}</span>
        <span class="bar-track"><span class="bar-fill" style="width:${width.toFixed(1)}%;background:${PALETTE[i % PALETTE.length]}"></span></span>
        <span class="val">${item.suffix ? `${item.value}${item.suffix}` : compactMoney(value)}</span>
      </div>`;
  }).join('') + `</div>`;
}

/** 在庫日数の分布 */
export function histogram(points) {
  const max = Math.max(1, ...points.map((p) => Number(p.units ?? 0)));
  return `<div class="bars">` + points.map((p, i) => {
    const width = (Number(p.units ?? 0) / max) * 100;
    return `
      <div class="bar-row">
        <span class="muted">${esc(p.label)}</span>
        <span class="bar-track"><span class="bar-fill" style="width:${width.toFixed(1)}%;background:${PALETTE[(i + 1) % PALETTE.length]}"></span></span>
        <span class="val">${p.units} 台</span>
      </div>`;
  }).join('') + `</div>`;
}

function esc(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
