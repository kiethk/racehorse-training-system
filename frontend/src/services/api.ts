/**
 * API service — REFERENCE IMPLEMENTATION
 *
 * Đây là file nền tảng dùng chung cho toàn bộ nhóm.
 * Mọi HTTP call tới backend đều phải đi qua các hàm ở đây,
 * không gọi fetch() trực tiếp ở component.
 */

const API_URL = process.env.NEXT_PUBLIC_API_URL;

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
let serverClockOffsetMs = 0;

export function getServerTime(): number {
  return Date.now() + serverClockOffsetMs;
}

export function getServerClockOffset(): number {
  return serverClockOffsetMs;
}

export function setServerClockOffset(serverTimeMs: number) {
  serverClockOffsetMs = serverTimeMs - Date.now();
}

let accessToken: string | null = null;

export function getAccessToken(): string | null {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
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

  let res = await fetch(`${API_URL}${path}`, finalOptions);

  const dateHeader = res.headers.get('date');
  if (dateHeader) {
    const serverMs = Date.parse(dateHeader);
    if (!Number.isNaN(serverMs)) {
      serverClockOffsetMs = serverMs - Date.now();
    }
  }

  if (res.status === 401 && !isAuthEndpoint) {
    if (!refreshPromise) {
      refreshPromise = (async () => {
        try {
          const refreshRes = await fetch(`${API_URL}/api/auth/refresh`, {
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
    }

    const refreshed = await refreshPromise;
    if (refreshed && accessToken) {
      // Retry original request exactly once
      headers.set('Authorization', `Bearer ${accessToken}`);
      finalOptions.headers = headers;
      res = await fetch(`${API_URL}${path}`, finalOptions);
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

/** Binary files use cookie authentication and the same refresh flow as JSON. */
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

/** Multipart upload uses the same cookie-backed API client as JSON requests. */
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
