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

export interface PreventiveDueItem {
  horseId: number;
  horseName: string;
  horseStatus: HorseStatus;
  protocol: PreventiveProtocolStatus;
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
  dosage: string;
  route?: string;
  instructions?: string;
  administeredOn?: string;
  source?: VaccinationSource;
}

export interface RecordVaccinationRequest {
  protocolId: number;
  dosage: string;
  route?: string;
  administeredOn?: string;
  instructions?: string;
}

export interface RecordVaccinationResult {
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
