import { apiGet } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { BioMetricType, BioSpcSeriesResponse, SurvivalEdaResponse, SurvivalStratifyBy } from '../types/medicalEda';

export const medicalEdaService = {
  getBioSpcSeries: async (horseId: number, metric: BioMetricType = 'TEMPERATURE'): Promise<BioSpcSeriesResponse> => {
    const res = await apiGet<ApiResponse<BioSpcSeriesResponse>>(
      `/api/medical-analytics/bio-spc/${horseId}?metric=${metric}`
    );
    return res.data;
  },

  getSurvivalAnalytics: async (stratifyBy: SurvivalStratifyBy = 'TREATMENT_PLAN'): Promise<SurvivalEdaResponse> => {
    const res = await apiGet<ApiResponse<SurvivalEdaResponse>>(
      `/api/medical-analytics/survival?stratifyBy=${stratifyBy}`
    );
    return res.data;
  },
};
