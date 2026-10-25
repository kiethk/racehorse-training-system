import type { HorseSex } from '@/types/horse';

export type StallStatus = 'AVAILABLE' | 'RESERVED' | 'OCCUPIED';
export type AreaType = 'REGULAR' | 'QUARANTINE';
/** A horse's lifecycle in the club. Health is not tracked here — see trainingDecision. */
export type HorseStatus = 'CANDIDATE' | 'ELIGIBLE' | 'REJECTED' | 'INACTIVE';

/** Whether the vet allows training or temporarily blocks it. Can train = ELIGIBLE and ALLOWED. */
export type TrainingDecision = 'ALLOWED' | 'BLOCKED';

export interface Area {
  id: number;
  code: string;          // "A", "B", "Q-A"
  name: string;
  type: AreaType;
  trainerId: number | null;
}

export interface StableStall {
  id: number;
  areaId: number;
  groomId: number | null;
  stallNumber: number;
  stallCode: string;     // "A01", "Q-A03"
  status: StallStatus;
}

export interface Horse {
  id: number;
  name: string;
  breed: string | null;
  sex: HorseSex | null;
  dateOfBirth: string | null;
  currentStatus: HorseStatus;
  statusReason?: string | null;
  statusReasonCode?: string | null;
  currentStallId: number | null;
  ownerId: number | null;
  trainingDecision?: TrainingDecision;
  /** Why the vet blocked training; null when ALLOWED. */
  trainingDecisionReason?: string | null;
  registrationNumber?: string | null;
  /** Existing nullable fields returned by the Horse API. */
  registryName?: string | null;
  stableLocation?: string | null;
  /** Protected photo URL path (/api/horses/{id}/photo); null when no photo on file. */
  imageUrl?: string | null;
  /** Creation timestamp */
  createdAt?: string | null;
  /** Racing specialty, returned when includeSpecialty=true; null/undefined otherwise. */
  specialty?: import('@/features/horses/types').HorseSpecialty | null;
}

export interface UserSummary {
  id: number;
  fullName: string;
  email: string;
  role: string;
}

export type HorseDeactivationReason =
  | 'RETIRED'
  | 'TRANSFERRED'
  | 'MEDICAL_DISCHARGE'
  | 'DECEASED'
  | 'OWNER_WITHDRAWAL'
  | 'OTHER';

export type HorseReactivationReason =
  | 'CLEARED_BY_VET'
  | 'RETURNED_FROM_LOAN'
  | 'MANAGEMENT_APPROVAL'
  | 'CORRECTION'
  | 'OTHER';

export const DEACTIVATION_REASON_OPTIONS: { value: HorseDeactivationReason; label: string }[] = [
  { value: 'RETIRED', label: 'Retired from training & competition' },
  { value: 'TRANSFERRED', label: 'Transferred to another facility / club' },
  { value: 'MEDICAL_DISCHARGE', label: 'Medical discharge / chronic injury' },
  { value: 'DECEASED', label: 'Deceased' },
  { value: 'OWNER_WITHDRAWAL', label: 'Withdrawn by owner request' },
  { value: 'OTHER', label: 'Other reason (specify below)...' },
];

export const REACTIVATION_REASON_OPTIONS: { value: HorseReactivationReason; label: string }[] = [
  { value: 'CLEARED_BY_VET', label: 'Cleared by veterinary team / recovered' },
  { value: 'RETURNED_FROM_LOAN', label: 'Returned from external facility / loan' },
  { value: 'MANAGEMENT_APPROVAL', label: 'Approved by club management' },
  { value: 'CORRECTION', label: 'Administrative correction' },
  { value: 'OTHER', label: 'Other reason (specify below)...' },
];
