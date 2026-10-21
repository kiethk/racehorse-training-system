import { apiGet, apiPost } from '@/services/api';
import type { InjuryRecord, HorseHealthSummary, CorrectInjuryRequest } from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const healthApi = {
  // Lấy tóm tắt sức khỏe của ngựa
  getHealthSummary: async (horseId: number): Promise<HorseHealthSummary> => {
    const res = await apiGet<ApiResponse<HorseHealthSummary> | HorseHealthSummary>(
      `/api/horses/${horseId}/health-summary`
    );
    if (res && 'data' in res && res.data) return res.data;
    return res as HorseHealthSummary;
  },

  // Lấy danh sách bệnh / chấn thương của ngựa
  getHorseInjuries: async (horseId: number): Promise<InjuryRecord[]> => {
    const res = await apiGet<ApiResponse<InjuryRecord[]> | InjuryRecord[]>(
      `/api/horses/${horseId}/injury-records`
    );
    if (res && 'data' in res && Array.isArray(res.data)) return res.data;
    if (Array.isArray(res)) return res;
    return [];
  },

  // Hiệu chỉnh bản ghi bệnh/chấn thương
  correctInjury: async (
    horseId: number,
    injuryId: number,
    request: CorrectInjuryRequest
  ): Promise<InjuryRecord> => {
    const res = await apiPost<ApiResponse<InjuryRecord> | InjuryRecord>(
      `/api/horses/${horseId}/injury-records/${injuryId}/corrections`,
      request
    );
    if (res && 'data' in res && res.data) return res.data;
    return res as InjuryRecord;
  },
};
