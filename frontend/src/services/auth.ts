import { apiGet, apiPost } from './api';
import type { ApiResponse } from '@/types/horse';
import type { AuthUser, LoginRequest, OwnerRegistrationRequest, OwnerRegistrationResponse } from '@/types/auth';

export async function login(request: LoginRequest): Promise<ApiResponse<AuthUser>> {
  const res = await apiPost<ApiResponse<AuthUser & { accessToken?: string, refreshToken?: string }>>('/api/auth/login', request);
  if (res.success && res.data.accessToken && res.data.refreshToken) {
    const { setTokens } = await import('./api');
    setTokens(res.data.accessToken, res.data.refreshToken);
  }
  return res;
}

export async function getCurrentUser(): Promise<ApiResponse<AuthUser>> {
  return apiGet<ApiResponse<AuthUser>>('/api/auth/me');
}

export async function logout(): Promise<ApiResponse<string>> {
  const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('rtms_refresh_token') : null;
  const res = await apiPost<ApiResponse<string>>('/api/auth/logout', { refreshToken });
  const { clearTokens } = await import('./api');
  clearTokens();
  return res;
}

/**
 * Register a new Horse Owner account.
 * The backend always assigns HORSE_OWNER role — role is NOT part of the request.
 * Throws an Error with a human-readable message on failure (e.g. duplicate email).
 */
export async function registerOwner(
  request: OwnerRegistrationRequest,
): Promise<ApiResponse<OwnerRegistrationResponse>> {
  return apiPost<ApiResponse<OwnerRegistrationResponse>>('/api/auth/register', request);
}
