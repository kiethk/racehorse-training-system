import type { SurfaceType } from '@/features/training/types';

/** Race-distance band, defined by the backend (the limits live in one place there). */
export type DistanceBand = 'SPRINT' | 'MILE' | 'MIDDLE' | 'STAYER';

/** Evidence behind a distance badge: the horse's rated workouts in one band. */
export interface DistanceStat {
  band: DistanceBand;
  workoutCount: number;
  averageRating: number;
  /** Null when no workout in the band recorded a top speed. */
  averageTopSpeedKmh: number | null;
}

/** Racing specialty inferred by the backend from completed workouts. Nothing is stored or typed in by hand. */
export interface HorseSpecialty {
  /** Rated workouts a group needs before it can be judged (from the backend, never hard-coded in the UI). */
  minWorkoutsPerGroup: number;
  /** Bands the horse does well in. Empty when none qualifies; may hold several. */
  strongDistances: DistanceBand[];
  /** Null when no surface clearly stands out. */
  preferredSurface: SurfaceType | null;
  distanceStats: DistanceStat[];
}

export interface HorseSummary {
  id: number;
  name: string;
  breed: string | null;
  dateOfBirth: string | null;
  currentStatus: string;
}

export interface HorsePedigree {
  id: number;
  horse: HorseSummary;
  sire: HorseSummary | null;
  dam: HorseSummary | null;
  registrationNumber?: string | null;
  registryName?: string | null;
  pedigreeNotes?: string | null;
  sireName?: string | null;
  sireRegistrationNumber?: string | null;
  damName?: string | null;
  damRegistrationNumber?: string | null;
}

export type AdmissionStatus =
  | 'GROOM_REVIEW'
  | 'WAITING_FOR_STALL'
  | 'WAITING_FOR_ARRIVAL'
  | 'ARRIVAL_EXPIRED'
  | 'VET_REVIEW'
  | 'TRAINER_REVIEW'
  | 'MANAGER_REVIEW'
  | 'APPROVED'
  | 'REJECTED';

/** Mirrors HorseAdmissionHistoryResponse — actual stored milestones only. */
export interface HorseAdmissionHistory {
  admissionId: number;
  horseId: number;
  status: AdmissionStatus;
  submittedAt: string | null;

  // Groom milestone
  groomId: number | null;
  groomName: string | null;
  groomReviewedAt: string | null;
  groomDecision: string | null;
  groomFeedback: string | null;

  // Arrival milestone
  arrivedAt: string | null;

  // Veterinarian milestone
  veterinarianId: number | null;
  vetName: string | null;
  vetReviewedAt: string | null;
  vetTrainingDecision: string | null;
  vetFeedback: string | null;

  // Trainer milestone
  trainerId: number | null;
  trainerName: string | null;
  trainerReviewedAt: string | null;
  trainerFeedback: string | null;

  // Manager milestone
  managerId: number | null;
  managerName: string | null;
  managerReviewedAt: string | null;
  managerDecision: string | null;
  managerFeedback: string | null;
}

export interface HorseHealthMetric {
  id: number;
  horseId: number;
  healthRecordId?: number | null;
  recordedAt: string;
  heartRate: number | null;
  temperature: number | null;
  weight: number | null;
  respiratoryRate: number | null;
  hydrationStatus?: string | null;
  bodyConditionScore?: number | null;
  notes?: string | null;
}


// ── Horse edit requests (owner asks, groom of the horse's stall approves) ──

export type HorseDocumentType =
  | 'HORSE_PHOTO'
  | 'REGISTRATION_DOCUMENT'
  | 'PEDIGREE_CERTIFICATE'
  | 'VACCINATION_RECORD'
  | 'DEWORMING_RECORD'
  | 'HEALTH_CERTIFICATE'
  | 'PREVIOUS_MEDICAL_RECORD'
  | 'PREVIOUS_INJURY_RECORD';

/** The mutable facts an owner may ask to change. UELN, sex, birth date and registry are not editable. */
export interface HorseEditFields {
  name: string | null;
  breed: string | null;
  sireName: string | null;
  sireRegistrationNumber: string | null;
  damName: string | null;
  damRegistrationNumber: string | null;
}

export type HorseEditRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED';

export interface HorseEditRequestDocument {
  id: number;
  documentType: HorseDocumentType;
  originalFileName: string | null;
  fileUrl: string;
}

export interface HorseEditRequest {
  id: number;
  horseId: number;
  horseName: string | null;
  horseRegistrationNumber: string | null;
  ownerId: number;
  ownerName: string | null;
  status: HorseEditRequestStatus;
  original: HorseEditFields;
  requested: HorseEditFields;
  documents: HorseEditRequestDocument[];
  reviewerId: number | null;
  reviewerName: string | null;
  reviewFeedback: string | null;
  reviewedAt: string | null;
  createdAt: string;
}

/** One document type of a horse: its current file, or an empty slot. */
export interface HorseDocumentSlot {
  documentType: HorseDocumentType;
  source: 'ADMISSION' | 'EDIT_REQUEST' | null;
  documentId: number | null;
  originalFileName: string | null;
  fileUrl: string | null;
  updatedAt: string | null;
}
