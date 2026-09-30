import { apiGet, apiPost } from '@/services/api';
import type { CreateRaceRegistrationRequest, RaceRegistrationResponse } from '../types';

interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export const racingService = {
  listMine: async (horseId?: number): Promise<RaceRegistrationResponse[]> => {
    const qs = horseId ? `?horseId=${horseId}` : '';
    return (await apiGet<ApiResponse<RaceRegistrationResponse[]>>(`/api/race-registrations${qs}`)).data;
  },

  getMine: async (id: number): Promise<RaceRegistrationResponse> => {
    return (await apiGet<ApiResponse<RaceRegistrationResponse>>(`/api/race-registrations/${id}`)).data;
  },

  create: async (body: CreateRaceRegistrationRequest): Promise<RaceRegistrationResponse> => {
    return (await apiPost<ApiResponse<RaceRegistrationResponse>>('/api/race-registrations', body)).data;
  },
};
