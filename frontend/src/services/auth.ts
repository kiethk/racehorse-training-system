import { apiGet, apiPost } from './api';
import type { ApiResponse } from '@/types/horse';
import type { AuthUser, LoginRequest, OwnerRegistrationRequest, OwnerRegistrationResponse } from '@/types/auth';

export async function login(request: LoginRequest): Promise<ApiResponse<AuthUser>> {
  return apiPost<ApiResponse<AuthUser>>('/api/auth/login', request);
}

export async function getCurrentUser(): Promise<ApiResponse<AuthUser>> {
  return apiGet<ApiResponse<AuthUser>>('/api/auth/me');
}

export async function logout(): Promise<ApiResponse<string>> {
  return apiPost<ApiResponse<string>>('/api/auth/logout', {});
}

/**
 * Register a new Horse Owner account.
 * The backend always assigns HORSE_OWNER role — role is NOT part of the request.
 * Throws an Error with a human-readable message on failure (e.g. duplicate email).
 */
export async function registerOwner(
  request: OwnerRegistrationRequest,
): Promise<ApiResponse<OwnerRegistrationResponse>> {
  const API_URL = process.env.NEXT_PUBLIC_API_URL;
  const res = await fetch(`${API_URL}/api/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  // Always parse the body so we can surface the backend's human-readable message
  const payload = await res.json().catch(() => null);

  if (!res.ok) {
    // Spring's ResponseStatusException puts the reason in `detail` (RFC 9457) or `message`
    const msg =
      payload?.detail ||
      payload?.message ||
      `Registration failed (${res.status})`;
    throw new Error(msg);
  }

  return payload as ApiResponse<OwnerRegistrationResponse>;
}
