/**
 * API service — REFERENCE IMPLEMENTATION
 *
 * Đây là file nền tảng dùng chung cho toàn bộ nhóm.
 * Mọi HTTP call tới backend đều phải đi qua các hàm ở đây,
 * không gọi fetch() trực tiếp ở component.
 */

const configuredApiUrl = process.env.NEXT_PUBLIC_API_URL?.trim();
const API_URL = (configuredApiUrl || (process.env.NODE_ENV === 'development' ? 'http://localhost:8080' : '')).replace(/\/+$/, '');

function getApiUrl(path: string): string {
  if (!API_URL) {
    throw new Error('The API connection is not configured. Set NEXT_PUBLIC_API_URL and rebuild the frontend.');
  }
  return `${API_URL}${path}`;
}

export class ApiError extends Error {
  constructor(public status: number, message: string, public errorCode?: string) {
    super(message);
    this.name = 'ApiError';
  }
}

async function responseError(res: Response): Promise<ApiError> {
  const payload = await res.json().catch(() => null);
  const msg = payload?.detail || payload?.message || `API error: ${res.status}`;
  return new ApiError(res.status, msg, payload?.errorCode);
}

let refreshPromise: Promise<boolean> | null = null;

export function createAuthenticatedEventSource(path: string): EventSource {
  return new EventSource(getApiUrl(path), { withCredentials: true });
}

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

export async function refreshAccessToken(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const refreshRes = await fetch(getApiUrl('/api/auth/refresh'), {
        method: 'POST',
        credentials: 'include',
        headers: { 'Content-Type': 'application/json' },
      });

      if (refreshRes.ok) {
        const data = await refreshRes.json();
        if (data?.data?.accessToken) {
          setAccessToken(data.data.accessToken);
          return true;
        }
      }
      setAccessToken(null);
      return false;
    } catch {
      setAccessToken(null);
      return false;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

async function doFetch(path: string, options: RequestInit): Promise<Response> {
  const isAuthEndpoint = [
    '/api/auth/login',
    '/api/auth/register',
    '/api/auth/refresh',
    '/api/auth/logout'
  ].includes(path);

  const finalOptions = { ...options };
  const headers = new Headers(options.headers || {});
  
  if (accessToken && !isAuthEndpoint) {
    headers.set('Authorization', `Bearer ${accessToken}`);
  }
  
  finalOptions.headers = headers;

  let res = await fetch(getApiUrl(path), finalOptions);

  if (res.status === 401 && !isAuthEndpoint) {
    const refreshed = await refreshAccessToken();
    if (refreshed && accessToken) {
      // Retry original request exactly once
      headers.set('Authorization', `Bearer ${accessToken}`);
      finalOptions.headers = headers;
      res = await fetch(getApiUrl(path), finalOptions);
    }
  }

  return res;
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await doFetch(path, {
    credentials: "include",
  });
  if (!res.ok) {
    throw await responseError(res);
  }
  return res.json();
}

/** Binary files use Bearer authentication and the same refresh flow as JSON. */
export async function apiGetBlob(path: string, signal?: AbortSignal): Promise<Blob> {
  let apiPath = path;
  let external = false;
  if (/^https?:\/\//i.test(path)) {
    const url = new URL(path);
    external = url.origin !== new URL(API_URL || window.location.origin).origin;
    apiPath = `${url.pathname}${url.search}`;
  }
  const res = external
    ? await fetch(path, { credentials: 'omit', signal })
    : await doFetch(apiPath, { credentials: 'include', signal });
  if (!res.ok) throw await responseError(res);
  return res.blob();
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const res = await doFetch(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw await responseError(res);
  }
  return res.json();
}

export async function apiPut<T>(path: string, body: unknown): Promise<T> {
  const res = await doFetch(path, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw await responseError(res);
  }
  return res.json();
}

/** Multipart upload uses the same Bearer API client as JSON requests. */
export async function apiUpload<T>(path: string, body: FormData): Promise<T> {
  const res = await doFetch(path, {
    method: "POST",
    credentials: "include",
    body,
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = payload?.detail || payload?.message || `API error: ${res.status}`;
    throw new ApiError(res.status, msg, payload?.errorCode);
  }
  return payload as T;
}

export async function apiPatch<T>(path: string, body?: unknown): Promise<T> {
  const res = await doFetch(path, {
    method: "PATCH",
    headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
    credentials: "include",
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
  if (!res.ok) {
    throw await responseError(res);
  }
  return res.json();
}
