import { getToken } from '../auth/auth';

const BASE = import.meta.env.VITE_API_BASE ?? '';

export class ApiError extends Error {
  constructor(public status: number, public code: string | undefined, message: string) {
    super(message);
  }
}

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json');
  const token = await getToken();
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const res = await fetch(`${BASE}${path}`, { ...init, headers });
  if (!res.ok) {
    let code: string | undefined;
    let message = res.status === 403 ? 'You do not have permission for this action.' : `Request failed (${res.status})`;
    try {
      const p = await res.json();
      code = p.code;
      if (p.detail) message = p.detail;
    } catch {
      /* ignore */
    }
    throw new ApiError(res.status, code, message);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

const send = (method: string) => <T>(path: string, body?: unknown) =>
  api<T>(path, { method, body: body === undefined ? undefined : JSON.stringify(body) });

export const post = send('POST');
export const put = send('PUT');
export const patch = send('PATCH');
export const del = send('DELETE');

export function qs(params: Record<string, string | number | boolean | undefined | null>): string {
  const sp = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => v !== undefined && v !== null && v !== '' && sp.set(k, String(v)));
  const s = sp.toString();
  return s ? `?${s}` : '';
}

export const money = (n?: number | null) =>
  n == null ? '' : new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(n);

export const dateTime = (iso?: string | null) =>
  iso ? new Date(iso).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '';

/** datetime-local <-> ISO helpers for schedule fields. */
export const toLocalInput = (iso?: string | null) => {
  if (!iso) return '';
  const d = new Date(iso);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
export const fromLocalInput = (v: string) => (v ? new Date(v).toISOString() : null);

export const label = (s: string) => s.replace(/_/g, ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
