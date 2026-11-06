// REST API クライアント
const BASE = '/api';

export class ApiError extends Error {
  constructor(status, payload) {
    super(payload?.message ?? `HTTP ${status}`);
    this.status = status;
    this.payload = payload ?? {};
    this.fieldErrors = payload?.fieldErrors ?? {};
  }
}

async function request(path, options = {}) {
  const init = {
    headers: { 'Accept': 'application/json' },
    ...options,
  };
  if (init.body && typeof init.body !== 'string') {
    init.body = JSON.stringify(init.body);
    init.headers['Content-Type'] = 'application/json';
  }

  const res = await fetch(BASE + path, init);
  if (res.status === 204) return null;

  const text = await res.text();
  let payload = null;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = { message: text };
    }
  }
  if (!res.ok) throw new ApiError(res.status, payload);
  return payload;
}

export function buildParams(query = {}) {
  const params = new URLSearchParams();
  const put = (key, value) => {
    if (value !== null && value !== undefined && value !== '') params.set(key, value);
  };
  put('search', query.search);
  put('status', query.status);
  put('stockType', query.stockType);
  put('saleType', query.saleType);
  put('manufacturer', query.manufacturer);
  put('stockDateFrom', query.stockDateFrom);
  put('stockDateTo', query.stockDateTo);
  put('sort', query.sort ?? 'stockDate');
  put('dir', query.dir ?? 'desc');
  put('page', query.page ?? 0);
  put('size', query.size ?? 25);
  return params;
}

export const api = {
  listCars(query) {
    return request(`/cars?${buildParams(query)}`);
  },
  getCar(id) {
    return request(`/cars/${id}`);
  },
  createCar(payload) {
    return request('/cars', { method: 'POST', body: payload });
  },
  updateCar(id, payload) {
    return request(`/cars/${id}`, { method: 'PUT', body: payload });
  },
  deleteCar(id) {
    return request(`/cars/${id}`, { method: 'DELETE' });
  },
  dashboard(months = 12) {
    return request(`/dashboard?months=${months}`);
  },
  meta() {
    return request('/meta');
  },
  exportUrl(query) {
    const params = buildParams(query);
    params.delete('page');
    params.delete('size');
    return `${BASE}/cars/export?${params}`;
  },
};
