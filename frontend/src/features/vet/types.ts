import type { CareSchedule } from '../admissions/types/index.ts';

export type CareScheduleResponse = CareSchedule;

export interface VetDashboardMetrics {
  activeEmergencies: number;
  admissionClearance: number;
  todayAppointments: number;
  trainingBlocked: number;
}

export interface VetWatchlistHorse {
  id: number;
  name: string;
  breed?: string | null;
  stallCode: string;
  reason: string;
}
