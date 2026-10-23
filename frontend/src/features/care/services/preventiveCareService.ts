import { apiGet, apiPatch, apiPost } from '@/services/api';
import type { CareScheduleResponse } from '../types';
import type {
  HorsePreventiveStatus,
  HorseVaccinationRequest,
  HorseVaccinationResult,
  PagedResult,
  PreventiveCoverage,
  PreventiveDueHorse,
  PreventiveDueStatus,
  PreventiveDueSummary,
  PreventiveProtocol,
  RescheduleRequest,
  RescheduleResponse,
} from '../types/preventive';

interface ApiResponse<T> {
  data: T;
}

export const preventiveCareService = {
  listProtocols: async (): Promise<PreventiveProtocol[]> =>
    (await apiGet<ApiResponse<PreventiveProtocol[]>>('/api/preventive-protocols')).data,

  /** "To do" tab: one row per horse, filtered and paged on the server. */
  listDueHorses: async (params: {
    statuses?: PreventiveDueStatus[];
    protocolId?: number;
    search?: string;
    page: number;
    size: number;
  }): Promise<PagedResult<PreventiveDueHorse>> => {
    const qs = new URLSearchParams();
    params.statuses?.forEach((status) => qs.append('status', status));
    if (params.protocolId != null) qs.set('protocolId', String(params.protocolId));
    if (params.search?.trim()) qs.set('search', params.search.trim());
    qs.set('page', String(params.page));
    qs.set('size', String(params.size));
    return (await apiGet<ApiResponse<PagedResult<PreventiveDueHorse>>>(`/api/preventive-care/due?${qs.toString()}`)).data;
  },

  dueSummary: async (): Promise<PreventiveDueSummary> =>
    (await apiGet<ApiResponse<PreventiveDueSummary>>('/api/preventive-care/due/summary')).data,

  coverage: async (): Promise<PreventiveCoverage[]> =>
    (await apiGet<ApiResponse<PreventiveCoverage[]>>('/api/preventive-care/coverage')).data,

  horseStatus: async (horseId: number): Promise<HorsePreventiveStatus> =>
    (await apiGet<ApiResponse<HorsePreventiveStatus>>(`/api/horses/${horseId}/preventive-status`)).data,

  recordForHorse: async (horseId: number, data: HorseVaccinationRequest): Promise<HorseVaccinationResult> =>
    (await apiPost<ApiResponse<HorseVaccinationResult>>(`/api/horses/${horseId}/vaccinations`, data)).data,

  reschedule: async (
    scheduleId: number,
    data: RescheduleRequest,
  ): Promise<RescheduleResponse<CareScheduleResponse>> =>
    (await apiPatch<ApiResponse<RescheduleResponse<CareScheduleResponse>>>(
      `/api/care-schedules/${scheduleId}/reschedule`,
      data,
    )).data,
};
