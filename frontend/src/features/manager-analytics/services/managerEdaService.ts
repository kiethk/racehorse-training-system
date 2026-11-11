import { apiGet } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { ManagerEdaOverviewResponse } from '../types/managerEda';

export const managerEdaService = {
  getOverview: async (days: number = 90): Promise<ManagerEdaOverviewResponse> => {
    const res = await apiGet<ApiResponse<ManagerEdaOverviewResponse>>(
      `/api/manager-analytics/eda/overview?days=${days}`
    );
    return res.data;
  },
};
