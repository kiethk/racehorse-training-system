import type {
  CareSchedule,
  CareScheduleStatus,
  CareType,
  CompleteCareScheduleRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  PageResponse,
  CreateNextScheduleRequest,
} from '@/features/admissions/types';

import type { MetricThreshold } from '@/components/ui/VitalsInput';

export type {
  CareSchedule,
  CareScheduleStatus,
  CareType,
  CompleteCareScheduleRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  PageResponse,
  CreateNextScheduleRequest,
};

export type VitalThreshold = MetricThreshold;

export interface VitalsData {
  heartRate?: number | string;
  temperature?: number | string;
  respiratoryRate?: number | string;
  weight?: number | string;
  bodyConditionScore?: number | string;
}

export interface LatestVitalsResponse {
  horseId: number;
  heartRate?: number | null;
  temperature?: number | null;
  respiratoryRate?: number | null;
  weight?: number | null;
  bodyConditionScore?: number | null;
  recordedAt?: string | null;
  thresholds?: Record<string, VitalThreshold>;
}

export interface BatchCareScheduleRowRequest {
  scheduleId: number;
  heartRate?: number;
  temperature?: number;
  respiratoryRate?: number;
  weight?: number;
  bodyConditionScore?: number;
  trainingDecision?: TrainingDecision;
  restrictionDetails?: string;
  nextScheduleDate?: string;
  notes?: string;
}

export interface BatchCompleteCareScheduleRequest {
  items: BatchCareScheduleRowRequest[];
}

export interface BatchCareScheduleRowResult {
  scheduleId: number;
  status: 'SUCCESS' | 'FAILED';
  errorMessage?: string | null;
  schedule?: CareSchedule | null;
}

export interface BatchCareScheduleResultResponse {
  total: number;
  successCount: number;
  failedCount: number;
  results: BatchCareScheduleRowResult[];
}

export interface CareTaskItem {
  id: number;
  horseId: number;
  horseName: string;
  stallCode?: string | null;
  careType: CareType;
  status: CareScheduleStatus;
  scheduledDate?: string | null;
  urgency: 'OVERDUE' | 'TODAY' | 'UPCOMING';
  description?: string | null;
  veterinarianName?: string | null;
  veterinarianId?: number | null;
}

export interface CareTasksSummary {
  overdueCount: number;
  todayCount: number;
  thisWeekCount: number;
  complianceRate: number;
}

export interface CareTaskBoardResponse {
  items: CareTaskItem[];
  summary: CareTasksSummary;
}

export interface CareTaskFilters {
  type?: CareType;
  status?: CareScheduleStatus;
  search?: string;
}
