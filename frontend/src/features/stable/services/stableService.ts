import { apiGet, apiPut } from '@/services/api';
import type { Area, Horse, StableStall, UserSummary, HorseStatus } from '../types';

interface ApiResponse<T> { success: boolean; data: T; message?: string; }

/**
 * The local putWithMessage helper was REMOVED — services/api.ts now has apiPut, and
 * responseError() there reads payload.message, so business errors (BR-07 "Stall
 * X is occupied by horse Y") are still shown verbatim.
 *
 * More important than tidiness: the old helper called fetch() itself, so it did NOT go through
 * doFetch() and skipped the automatic token refresh on 401. If the JWT expired
 * while viewing the stable map, assigning a horse failed outright,
 * whereas every call through apiGet/apiPut refreshes and retries once.
 *
 * The {} body is a placeholder: the two endpoints below take @RequestParam, not
 * @RequestBody, so Spring ignores the body.
 */
export const stableApi = {
  getAreas: async (): Promise<Area[]> =>
    (await apiGet<ApiResponse<Area[]>>('/api/areas')).data,

  getStalls: async (areaCode?: string): Promise<StableStall[]> => {
    const url = areaCode ? `/api/stalls?areaCode=${areaCode}` : '/api/stalls';
    return (await apiGet<ApiResponse<StableStall[]>>(url)).data;
  },

  /** mine=true -> only horses in the block the Trainer is responsible for (BE-2.1). */
  /**
   * mine       -> horses in the Trainer's block (already assigned to a stall).
   * unassigned -> horses NOT yet assigned to a stall, for the assign-horse dialog.
   *
   * The two flags are mutually exclusive: "my block" is derived FROM the stall, so a horse without
   * a stall never satisfies mine=true. Passing both always returns an empty list.
   */
  getHorses: async (opts?: {
    mine?: boolean;
    unassigned?: boolean;
    status?: HorseStatus;
  }): Promise<Horse[]> => {
    const params = new URLSearchParams();
    if (opts?.mine) params.set('mine', 'true');
    if (opts?.unassigned) params.set('unassigned', 'true');
    if (opts?.status) params.set('status', opts.status);
    const qs = params.toString();
    return (await apiGet<ApiResponse<Horse[]>>(`/api/horses${qs ? `?${qs}` : ''}`)).data;
  },

  getGrooms: async (): Promise<UserSummary[]> =>
    (await apiGet<ApiResponse<UserSummary[]>>('/api/users?role=GROOM')).data,

  /** NOTE: the backend takes @RequestParam, NOT a body. */
  assignHorseToStall: async (horseId: number, stallId: number): Promise<void> => {
    await apiPut(`/api/horses/${horseId}/assign-stall?stallId=${stallId}`, {});
  },

  /**
   * Remove a horse from its stall — leave stallId empty.
   *
   * Sessions that have not happened yet become "no Groom assigned".
   */
  unassignHorseFromStall: async (horseId: number): Promise<void> => {
    await apiPut(`/api/horses/${horseId}/assign-stall`, {});
  },

  // REMOVED: assignGroomToStall — assigning a Groom to a stall moved to the Club
  // Manager (V63). PUT /api/stalls/{id}/assign-groom still exists but
  // requires the STALL_GROOM_ASSIGN permission, which Head Trainers do not have, so calling it from here
  // only returns 403. Trainers still move a horse to another stall with assignHorseToStall.
};
