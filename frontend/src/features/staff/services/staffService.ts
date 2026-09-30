import { apiGet } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { StaffSummary, StaffCreationRequest } from '../types';

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export async function getStaffList(): Promise<ApiResponse<StaffSummary[]>> {
  return apiGet<ApiResponse<StaffSummary[]>>('/api/staff');
}

export async function createStaff(request: StaffCreationRequest): Promise<ApiResponse<StaffSummary>> {
  const res = await fetch(`${API_URL}/api/staff`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(request),
  });

  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = payload?.detail || payload?.message || `Failed to create staff (${res.status})`;
    throw new Error(msg);
  }
  return payload as ApiResponse<StaffSummary>;
}

export async function updateStaffStatus(id: number, active: boolean): Promise<ApiResponse<StaffSummary>> {
  const res = await fetch(`${API_URL}/api/staff/${id}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ active }),
  });

  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = payload?.detail || payload?.message || `Failed to update status (${res.status})`;
    throw new Error(msg);
  }
  return payload as ApiResponse<StaffSummary>;
}
