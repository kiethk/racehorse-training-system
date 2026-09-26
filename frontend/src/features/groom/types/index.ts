export type TaskSource = 'SOP' | 'WORKOUT' | 'PREVENTIVE_CARE';
export type TaskStatus = 'PENDING' | 'COMPLETED';

export interface TodayTaskItem {
  source: TaskSource;
  refId: number;
  startTime: string; // "05:00:00"
  endTime: string | null;
  horseId: number;
  horseName: string;
  title: string;
  note: string | null;
  status: TaskStatus;
  actionable: boolean;
}

export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type IncidentStatus = 'REPORTED' | 'ACKNOWLEDGED' | 'RESOLVED';

export interface IncidentReport {
  id: number;
  horseId: number;
  horseName?: string;
  groomId: number;
  groomName?: string;
  title: string;
  description: string;
  imageUrl: string | null;
  severity: IncidentSeverity;
  status: IncidentStatus;
  handledById?: number | null;
  handlerId?: number | null;
  handlerName?: string | null;
  handlerNote: string | null;
  reportedAt: string;
  handledAt: string | null;
}

export interface CreateIncidentRequest {
  horseId: number;
  title: string;
  description: string;
  imageUrl?: string | null;
  severity: IncidentSeverity;
}
