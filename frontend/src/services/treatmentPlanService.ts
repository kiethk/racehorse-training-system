import { apiGet } from './api';
import type { TreatmentPlan } from '@/features/care/types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const treatmentPlanService = {
  getActiveByHorseId: async (horseId: number): Promise<TreatmentPlan | null> => {
    try {
      const res = await apiGet<ApiResponse<TreatmentPlan | null>>(`/api/treatment-plans/horses/${horseId}/active`);
      return res?.data ?? null;
    } catch {
      return null;
    }
  },
};

