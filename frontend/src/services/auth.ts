import { apiGet, apiPost } from './api';
import type { ApiResponse } from '@/types/horse';
import type { AuthUser, LoginRequest, OwnerRegistrationRequest, OwnerRegistrationResponse } from '@/types/auth';

export async function login(request: LoginRequest): Promise<ApiResponse<AuthUser>> {
  const res = await apiPost<ApiResponse<AuthUser & { accessToken?: string }>>('/api/auth/login', request);
  if (res.success && res.data.accessToken) {
    const { setAccessToken } = await import('./api');
    setAccessToken(res.data.accessToken);
  }
  return res;
}

export async function getCurrentUser(): Promise<ApiResponse<AuthUser>> {
  return apiGet<ApiResponse<AuthUser>>('/api/auth/me');
}

export async function logout(): Promise<ApiResponse<string>> {
  try {
    return await apiPost<ApiResponse<string>>('/api/auth/logout', {});
  } finally {
    const { setAccessToken } = await import('./api');
    setAccessToken(null);
  }
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
