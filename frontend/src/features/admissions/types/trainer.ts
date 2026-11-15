import type {
  AdmissionDetailResponse,
  AdmissionSummaryResponse,
  HealthRecord,
  HorseHealthMetricResponse,
} from './index';

/**
 * Matches dto/TrainerAdmissionQueueResponse.java.
 *
 * The backend already filters by the signed-in Trainer, so the screen does NOT filter by
 * trainerId or status again — it only picks the right array for the tab.
 */
export interface TrainerAdmissionQueue {
  /** Admissions in TRAINER_REVIEW that the system assigned to me. */
  pending: AdmissionSummaryResponse[];
  /** Admissions I have reviewed — every later status, including rejected by the Manager. */
  reviewed: AdmissionSummaryResponse[];
  /** Horses I look after now: horses in my areas plus admissions assigned to me that are not finished. */
  activeHorseCount: number;
  /** Load limit; at this count the system assigns me no new admissions. */
  horseLimit: number;
  atCapacity: boolean;
}

export interface HorseSummary {
  id: number;
  name: string;
  breed: string | null;
  sex: import('@/types/horse').HorseSex | null;
  dateOfBirth: string | null;
  currentStatus: string;
  currentStallId: number | null;
  registryName: string | null;
  registrationNumber: string | null;
}

export interface TrainerAdmissionView {
  admission: AdmissionDetailResponse;
  /** null if the Groom step has not created the Horse record. The feedback CANNOT be submitted then. */
  horse: HorseSummary | null;
  /** The vet's examination data. */
  healthRecords: HealthRecord[];
  /** Vital signs data (temperature, heart rate, respiratory rate...). */
  healthMetrics: HorseHealthMetricResponse[];
}

/** Matches dto/TrainerAdmissionReviewRequest.java: one required written feedback. */
export interface TrainerReviewRequest {
  trainerFeedback: string;
}
