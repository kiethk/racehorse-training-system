import { apiGet, apiPatch, apiPost } from '@/services/api';
import type { CareScheduleResponse } from '../types';
import type {
  HorsePreventiveStatus,
  PreventiveCoverage,
  PreventiveDueItem,
  PreventiveDueStatus,
  PreventiveProtocol,
  RecordVaccinationRequest,
  RecordVaccinationResult,
  RescheduleRequest,
  RescheduleResponse,
} from '../types/preventive';

interface ApiResponse<T> {
  data: T;
}

export const preventiveCareService = {
  listProtocols: async (): Promise<PreventiveProtocol[]> =>
    (await apiGet<ApiResponse<PreventiveProtocol[]>>('/api/preventive-protocols')).data,

  listDue: async (params?: {
    statuses?: PreventiveDueStatus[];
    withinDays?: number;
    protocolId?: number;
  }): Promise<PreventiveDueItem[]> => {
    const qs = new URLSearchParams();
    params?.statuses?.forEach((status) => qs.append('status', status));
    if (params?.withinDays != null) qs.set('withinDays', String(params.withinDays));
    if (params?.protocolId != null) qs.set('protocolId', String(params.protocolId));
    const query = qs.toString() ? `?${qs.toString()}` : '';
    return (await apiGet<ApiResponse<PreventiveDueItem[]>>(`/api/preventive-care/due${query}`)).data;
  },

  coverage: async (): Promise<PreventiveCoverage[]> =>
    (await apiGet<ApiResponse<PreventiveCoverage[]>>('/api/preventive-care/coverage')).data,

  horseStatus: async (horseId: number): Promise<HorsePreventiveStatus> =>
    (await apiGet<ApiResponse<HorsePreventiveStatus>>(`/api/horses/${horseId}/preventive-status`)).data,

  recordVaccination: async (horseId: number, data: RecordVaccinationRequest): Promise<RecordVaccinationResult> =>
    (await apiPost<ApiResponse<RecordVaccinationResult>>(`/api/horses/${horseId}/vaccinations`, data)).data,

  reschedule: async (
    scheduleId: number,
    data: RescheduleRequest,
  ): Promise<RescheduleResponse<CareScheduleResponse>> =>
    (await apiPatch<ApiResponse<RescheduleResponse<CareScheduleResponse>>>(
      `/api/care-schedules/${scheduleId}/reschedule`,
      data,
    )).data,
};
