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
  /** What the Groom wrote when completing the task. Routine (SOP) tasks only. */
  completionNote?: string | null;
  /** API path of the photo taken on completion; render it with <ProtectedImage>. */
  completionImageUrl?: string | null;
}

export type IncidentSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
/**
 * Must match enums/IncidentStatus.java and the chk_incident_status constraint
 * in migration V45. A mismatch causes a 400 response when filtering by status;
 * TypeScript cannot catch it because this validation happens in the backend.
 */
export type IncidentStatus = 'REPORTED' | 'IN_REVIEW' | 'RESOLVED' | 'DISMISSED';

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
