import { apiGet, apiPost } from '@/services/api';
import type {
  CareSchedule,
  CareTaskBoardResponse,
  CareTaskFilters,
  CompleteCareScheduleRequest,
  LatestVitalsResponse,
  BatchCompleteCareScheduleRequest,
  BatchCareScheduleResultResponse,
} from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const careApi = {
  getCareTasks: async (filters?: CareTaskFilters): Promise<CareTaskBoardResponse> => {
    const params = new URLSearchParams();
    if (filters?.type) params.set('type', filters.type);
    if (filters?.status) params.set('status', filters.status);
    if (filters?.search?.trim()) params.set('search', filters.search.trim());
    const query = params.toString() ? `?${params.toString()}` : '';
    const res = await apiGet<ApiResponse<CareTaskBoardResponse>>(`/api/care-schedules/tasks${query}`);
    return res.data;
  },

  getLatestVitals: async (horseId: number): Promise<LatestVitalsResponse> => {
    const res = await apiGet<ApiResponse<LatestVitalsResponse>>(`/api/care-schedules/horses/${horseId}/latest-vitals`);
    return res.data;
  },

  startSchedule: async (id: number): Promise<CareSchedule> => {
    const res = await apiPost<ApiResponse<CareSchedule>>(`/api/care-schedules/${id}/start`, {});
    return res.data;
  },

  completeSchedule: async (id: number, request: CompleteCareScheduleRequest): Promise<CareSchedule> => {
    const res = await apiPost<ApiResponse<CareSchedule>>(`/api/care-schedules/${id}/complete`, request);
    return res.data;
  },

  batchComplete: async (request: BatchCompleteCareScheduleRequest): Promise<BatchCareScheduleResultResponse> => {
    const res = await apiPost<ApiResponse<BatchCareScheduleResultResponse>>('/api/care-schedules/batch-complete', request);
    return res.data;
  },
};
