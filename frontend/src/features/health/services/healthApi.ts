import { apiGet, apiPost } from '@/services/api';
import type { HorseHealthMetric } from '@/features/horses/types';
import type { HealthRecord, InjuryRecord, HorseHealthSummary, CorrectInjuryRequest } from '../types';

export interface CreateInjuryPayload {
  healthRecordId: number;
  injuryType: string;
  bodyRegionId: number;
  severity: string;
  description?: string;
  diagnosedAt: string;
  positionX?: number | null;
  positionY?: number | null;
  positionZ?: number | null;
  zoneId?: string | null;
  localX?: number | null;
  localY?: number | null;
  localZ?: number | null;
  normalX?: number | null;
  normalY?: number | null;
  normalZ?: number | null;
  meshName?: string | null;
}
import type { HorsePreventiveStatus } from '@/features/care/types/preventive';

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

  // Lấy các chấn thương còn ACTIVE của ngựa (để render marker trên model)
  getActiveInjuries: async (horseId: number): Promise<InjuryRecord[]> => {
    const res = await apiGet<ApiResponse<InjuryRecord[]> | InjuryRecord[]>(
      `/api/horses/${horseId}/injury-records/active`
    );
    if (res && 'data' in res && Array.isArray(res.data)) return res.data;
    if (Array.isArray(res)) return res;
    return [];
  },

  // Tạo bản ghi chấn thương mới (kèm marker 3D)
  createInjury: async (
    horseId: number,
    request: CreateInjuryPayload
  ): Promise<InjuryRecord> => {
    const res = await apiPost<ApiResponse<InjuryRecord> | InjuryRecord>(
      `/api/horses/${horseId}/injury-records`,
      request
    );
    if (res && 'data' in res && res.data) return res.data;
    return res as InjuryRecord;
  },

  // Đánh dấu chấn thương đã hồi phục (marker biến mất)
  recoverInjury: async (horseId: number, injuryId: number): Promise<InjuryRecord> => {
    const res = await apiPost<ApiResponse<InjuryRecord> | InjuryRecord>(
      `/api/horses/${horseId}/injury-records/${injuryId}/recover`,
      {}
    );
    if (res && 'data' in res && res.data) return res.data;
    return res as InjuryRecord;
  },

  // Chỉ số sinh tồn (vitals) của các ca khám gần nhất
  getHealthMetrics: async (horseId: number): Promise<HorseHealthMetric[]> => {
    const res = await apiGet<ApiResponse<HorseHealthMetric[]> | HorseHealthMetric[]>(
      `/api/horses/${horseId}/health-metrics`
    );
    if (res && 'data' in res && Array.isArray(res.data)) return res.data;
    if (Array.isArray(res)) return res;
    return [];
  },

  // Lịch sử khám sức khỏe (health history), mới nhất trước
  getHealthRecords: async (horseId: number): Promise<HealthRecord[]> => {
    const res = await apiGet<ApiResponse<HealthRecord[]> | HealthRecord[]>(
      `/api/horses/${horseId}/health-records`
    );
    if (res && 'data' in res && Array.isArray(res.data)) return res.data;
    if (Array.isArray(res)) return res;
    return [];
  },

  // Trạng thái tiêm phòng/preventive theo từng loại vaccine
  getPreventiveStatus: async (horseId: number): Promise<HorsePreventiveStatus> => {
    const res = await apiGet<ApiResponse<HorsePreventiveStatus> | HorsePreventiveStatus>(
      `/api/horses/${horseId}/preventive-status`
    );
    if (res && 'data' in res && res.data) return res.data;
    return res as HorsePreventiveStatus;
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
