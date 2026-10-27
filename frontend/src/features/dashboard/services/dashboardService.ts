import { apiGet } from '@/services/api';
import type { ApiResponse } from '@/types/horse';
import type { DashboardHorse, HealthTrend, PerformanceTrend, ManagerDashboardSummary } from '../types';

/** The data of the role dashboards. The backend scopes every answer to the horses the signed-in user may see. */
export const dashboardService = {
  getHorses: async (): Promise<DashboardHorse[]> =>
    (await apiGet<ApiResponse<DashboardHorse[]>>('/api/training-plans/dashboard')).data,

  getPerformanceTrend: async (): Promise<PerformanceTrend> =>
    (await apiGet<ApiResponse<PerformanceTrend>>('/api/training-plans/dashboard/performance-trend')).data,

  getHealthTrend: async (): Promise<HealthTrend> =>
    (await apiGet<ApiResponse<HealthTrend>>('/api/health-metrics/trend')).data,

  getManagerSummary: async (): Promise<ManagerDashboardSummary> =>
    (await apiGet<ApiResponse<ManagerDashboardSummary>>('/api/dashboard/manager')).data,
};
