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

export async function apiGet<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    credentials: "include",
  });
  if (!res.ok) {
    throw new Error(`API error: ${res.status}`);
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
    throw new Error(`API error: ${res.status}`);
  }
  return res.json();
}