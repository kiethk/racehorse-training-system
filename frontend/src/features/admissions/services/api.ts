import { apiGet } from '@/services/api';
import { AdmissionSummaryResponse } from '../types';

interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export const admissionsApi = {
  getManagerReviewQueue: async (): Promise<AdmissionSummaryResponse[]> => {
    const response = await apiGet<ApiResponse<AdmissionSummaryResponse[]>>('/api/admissions?status=MANAGER_REVIEW');
    return response.data; 
  },
};
