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
import { unwrapArray, unwrapData } from '../lib/unwrapResponse';

export const preventiveCareService = {
  listProtocols: async (): Promise<PreventiveProtocol[]> => {
    const res = await apiGet<unknown>('/api/preventive-protocols');
    return unwrapArray<PreventiveProtocol>(res);
  },

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
    const res = await apiGet<unknown>(`/api/preventive-care/due?${qs.toString()}`);
    return unwrapData<PagedResult<PreventiveDueHorse>>(res);
  },

  dueSummary: async (): Promise<PreventiveDueSummary> => {
    const res = await apiGet<unknown>('/api/preventive-care/due/summary');
    return unwrapData<PreventiveDueSummary>(res);
  },

  coverage: async (): Promise<PreventiveCoverage[]> => {
    const res = await apiGet<unknown>('/api/preventive-care/coverage');
    return unwrapArray<PreventiveCoverage>(res);
  },

  horseStatus: async (horseId: number): Promise<HorsePreventiveStatus> => {
    const res = await apiGet<unknown>(`/api/horses/${horseId}/preventive-status`);
    return unwrapData<HorsePreventiveStatus>(res);
  },

  recordForHorse: async (horseId: number, data: HorseVaccinationRequest): Promise<HorseVaccinationResult> => {
    const res = await apiPost<unknown>(`/api/horses/${horseId}/vaccinations`, data);
    return unwrapData<HorseVaccinationResult>(res);
  },

  reschedule: async (
    scheduleId: number,
    data: RescheduleRequest,
  ): Promise<RescheduleResponse<CareScheduleResponse>> => {
    const res = await apiPatch<unknown>(
      `/api/care-schedules/${scheduleId}/reschedule`,
      data,
    );
    return unwrapData<RescheduleResponse<CareScheduleResponse>>(res);
  },
};
