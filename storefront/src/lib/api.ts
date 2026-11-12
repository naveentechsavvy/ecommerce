import { getToken } from '../auth/keycloak';

const BASE = import.meta.env.VITE_API_BASE ?? '';

/** Error carrying the backend's RFC 7807 problem detail + stable code. */
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
    let message = `Request failed (${res.status})`;
    try {
      const problem = await res.json();
      code = problem.code;
      message = problem.detail || problem.title || message;
    } catch {
      /* non-JSON error body */
    }
    throw new ApiError(res.status, code, message);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

export const post = <T>(path: string, body: unknown) => api<T>(path, { method: 'POST', body: JSON.stringify(body) });

export function qs(params: Record<string, string | number | boolean | string[] | undefined | null>): string {
  const sp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v === undefined || v === null || v === '') continue;
    if (Array.isArray(v)) v.forEach((x) => sp.append(k, x));
    else sp.set(k, String(v));
  }
  const s = sp.toString();
  return s ? `?${s}` : '';
}
