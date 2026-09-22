import { apiGet, apiPost } from './api';
import type { ApiResponse } from '@/types/horse';
import type { AuthUser, LoginRequest } from '@/types/auth';

export async function login(request: LoginRequest): Promise<ApiResponse<AuthUser>> {
  return apiPost<ApiResponse<AuthUser>>('/api/auth/login', request);
}

export async function getCurrentUser(): Promise<ApiResponse<AuthUser>> {
  return apiGet<ApiResponse<AuthUser>>('/api/auth/me');
}

export async function logout(): Promise<ApiResponse<string>> {
  return apiPost<ApiResponse<string>>('/api/auth/logout', {});
}
