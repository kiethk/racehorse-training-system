import { apiGet } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { FleetEdaResponse, IndividualHorseEdaResponse } from '../types/eda';

export const edaAnalyticsService = {
  getFleetEdaAnalytics: async (days: number = 180): Promise<FleetEdaResponse> => {
    const res = await apiGet<ApiResponse<FleetEdaResponse>>(`/api/analytics/eda/fleet?days=${days}`);
    return res.data;
  },

  getIndividualEdaAnalytics: async (horseId: number, days: number = 365): Promise<IndividualHorseEdaResponse> => {
    const res = await apiGet<ApiResponse<IndividualHorseEdaResponse>>(`/api/analytics/eda/horses/${horseId}?days=${days}`);
    return res.data;
  },
};
