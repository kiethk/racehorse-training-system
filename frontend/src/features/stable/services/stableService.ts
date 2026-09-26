import { apiGet } from '@/services/api';
import type { Area, Horse, StableStall, UserSummary, HorseStatus } from '../types';

interface ApiResponse<T> { success: boolean; data: T; message?: string; }

const API_URL = process.env.NEXT_PUBLIC_API_URL;

/**
 * TODO(nhóm): xoá khi services/api.ts giữ lại message lỗi — xem PLAN_05 phần D.
 *
 * Màn hình này cần hiện nguyên văn lỗi BR-06 ("Groom X đã phụ trách 3/3 chuồng")
 * và BR-07. apiPut chưa tồn tại, apiPost thì vứt body lỗi.
 */
async function putWithMessage<T>(path: string): Promise<T> {
  const res = await fetch(`${API_URL}${path}`, {
    method: 'PUT',
    credentials: 'include',
  });
  const payload = await res.json().catch(() => null);
  if (!res.ok) {
    throw new Error(payload?.message || `API error: ${res.status}`);
  }
  return payload as T;
}

export const stableApi = {
  getAreas: async (): Promise<Area[]> =>
    (await apiGet<ApiResponse<Area[]>>('/api/areas')).data,

  getStalls: async (areaCode?: string): Promise<StableStall[]> => {
    const url = areaCode ? `/api/stalls?areaCode=${areaCode}` : '/api/stalls';
    return (await apiGet<ApiResponse<StableStall[]>>(url)).data;
  },

  /** mine=true -> chỉ ngựa trong khu Trainer phụ trách (BE-2.1). */
  getHorses: async (opts?: { mine?: boolean; status?: HorseStatus }): Promise<Horse[]> => {
    const params = new URLSearchParams();
    if (opts?.mine) params.set('mine', 'true');
    if (opts?.status) params.set('status', opts.status);
    const qs = params.toString();
    return (await apiGet<ApiResponse<Horse[]>>(`/api/horses${qs ? `?${qs}` : ''}`)).data;
  },

  getGrooms: async (): Promise<UserSummary[]> =>
    (await apiGet<ApiResponse<UserSummary[]>>('/api/users?role=GROOM')).data,

  /** LƯU Ý: backend nhận @RequestParam, KHÔNG phải body. */
  assignHorseToStall: async (horseId: number, stallId: number): Promise<void> => {
    await putWithMessage(`/api/horses/${horseId}/assign-stall?stallId=${stallId}`);
  },

  /** Bỏ trống groomId = gỡ Groom khỏi chuồng. */
  assignGroomToStall: async (stallId: number, groomId: number | null): Promise<void> => {
    const qs = groomId == null ? '' : `?groomId=${groomId}`;
    await putWithMessage(`/api/stalls/${stallId}/assign-groom${qs}`);
  },
};
