import { apiGet } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { Area, StableStall } from '@/features/stable/types';

export interface UserSummaryResponse {
  id: number;
  fullName: string;
  email: string;
  role: string | null;
}

export const managerHorseService = {
  /** GET /api/stalls/{id} — requires STABLE_STALL_VIEW. */
  getStallById: async (stallId: number): Promise<StableStall> =>
    (await apiGet<ApiResponse<StableStall>>(`/api/stalls/${stallId}`)).data,

  /** GET /api/areas/{id} — requires AREA_VIEW. */
  getAreaById: async (areaId: number): Promise<Area> =>
    (await apiGet<ApiResponse<Area>>(`/api/areas/${areaId}`)).data,

  /** GET /api/users/{id} — requires USER_VIEW. */
  getUserById: async (userId: number): Promise<UserSummaryResponse> =>
    (await apiGet<ApiResponse<UserSummaryResponse>>(`/api/users/${userId}`)).data,
};
