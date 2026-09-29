/**
 * API service — REFERENCE IMPLEMENTATION
 *
 * Đây là file nền tảng dùng chung cho toàn bộ nhóm.
 * Mọi HTTP call tới backend đều phải đi qua các hàm ở đây,
 * không gọi fetch() trực tiếp ở component.
 *
 * Cách dùng:
 *   import { apiGet, apiPost } from "@/services/api";
 *   const data = await apiGet<ApiResponse<Horse[]>>("/api/horses");
 *
 * Convention:
 *   - GET  → apiGet<ResponseType>(path)
 *   - POST → apiPost<ResponseType>(path, body)
 *   - Thêm apiPut / apiDelete theo cùng pattern khi cần
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
  return new ApiError(res.status, payload?.message || `API error: ${res.status}`, payload?.errorCode);
}

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    credentials: "include",
  });
  if (!res.ok) {
    throw await responseError(res);
  }
  return res.json();
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
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
  const res = await fetch(`${API_URL}${path}`, {
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
  const res = await fetch(`${API_URL}${path}`, {
    method: "POST",
    credentials: "include",
    body,
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new ApiError(res.status, payload?.message || `API error: ${res.status}`, payload?.errorCode);
  }
  return payload as T;
}
