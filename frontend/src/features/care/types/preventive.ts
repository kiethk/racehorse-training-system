/** Mirrors the backend preventive care DTOs (docs/preventive-care-flow.md). */

export type ProtocolCategory = 'VACCINE' | 'DEWORMING';
export type PreventiveDueStatus = 'OK' | 'DUE_SOON' | 'OVERDUE' | 'MISSING' | 'NONE';
export type VaccinationSource = 'ADMINISTERED' | 'HISTORICAL';
export type CareSchedulePurpose = 'FOLLOW_UP' | 'PREVENTIVE';
export type RescheduleReason = 'RACE_CONFLICT' | 'UNDER_TREATMENT' | 'OWNER_REQUEST' | 'WEATHER' | 'OTHER';
export type HorseStatus = 'CANDIDATE' | 'ELIGIBLE' | 'REJECTED';

export interface PreventiveProtocol {
  id: number;
  code: string;
  name: string;
  category: ProtocolCategory;
  intervalDays: number;
  warningDays: number;
  mandatory: boolean;
  defaultDosage: string | null;
  defaultRoute: string | null;
}

export interface PreventiveProtocolStatus {
  horseId: number;
  protocolId: number;
  code: string;
  name: string;
  category: ProtocolCategory;
  mandatory: boolean;
  lastDoseOn: string | null;
  nextDueOn: string | null;
  status: PreventiveDueStatus;
}

export interface PreventiveDueHorse {
  horseId: number;
  horseName: string;
  horseStatus: HorseStatus;
  /** Only the protocols that match the current filter, most urgent first. */
  protocols: PreventiveProtocolStatus[];
  /** Null when the most urgent protocol has never been recorded. */
  earliestDueOn: string | null;
  worstStatus: PreventiveDueStatus;
}

export interface PreventiveDueSummary {
  overdue: number;
  dueSoon: number;
  horses: number;
}

export interface PagedResult<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface PreventiveCoverage {
  protocolId: number;
  code: string;
  name: string;
  mandatory: boolean;
  totalHorses: number;
  ok: number;
  dueSoon: number;
  overdue: number;
  missing: number;
  none: number;
}

export interface PreventiveMilestone {
  id: number;
  horseId: number;
  veterinarianId: number | null;
  status: 'REQUESTED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  scheduledAt: string | null;
  requestedAt: string | null;
  description: string | null;
  purpose: CareSchedulePurpose;
  rescheduleReason: RescheduleReason | null;
}

export interface HorsePreventiveStatus {
  horseId: number;
  horseName: string;
  protocols: PreventiveProtocolStatus[];
  nextMilestone: PreventiveMilestone | null;
}

export interface VaccinationRequest {
  protocolId: number;
  /** Required for a dose given in the exam; omitted for a passport last-dose date. */
  dosage?: string;
  route?: string;
  instructions?: string;
  administeredOn?: string;
  source?: VaccinationSource;
}

export interface HorseVaccinationRequest {
  administeredOn?: string;
  instructions?: string;
  doses: { protocolId: number; dosage: string; route?: string }[];
}

export interface HorseVaccinationResult {
  horseId: number;
  healthRecordId: number;
}

export interface RescheduleRequest {
  newDate: string;
  reason: RescheduleReason;
  note?: string;
}

export interface RescheduleResponse<Schedule> {
  schedule: Schedule;
  warnings: string[];
}
