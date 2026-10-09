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
  /** Admissions I have evaluated — every later status, including rejected by the Manager. */
  reviewed: AdmissionSummaryResponse[];
}

/**
 * Matches enums/RacingReadinessStatus.java.
 * The DB has CHECK constraint chk_rra_status that accepts exactly these 3 values.
 *
 * UNSUITABLE was renamed from NOT_READY (V47) because the old name could not be told apart from
 * NEEDS_MORE_TRAINING. This is the ONLY channel for the Trainer to signal "we should not
 * take this horse" — the Trainer cannot reject an admission.
 */
export type RacingReadinessStatus = 'READY' | 'NEEDS_MORE_TRAINING' | 'UNSUITABLE';

export interface RacingReadinessAssessment {
  id: number;
  horseId: number;
  readinessStatus: RacingReadinessStatus;
  /** ALWAYS null at intake — the horse is in quarantine, fitness cannot be measured. */
  fitnessScore: number | null;
  conformationScore: number | null;
  temperamentScore: number | null;
  gaitQualityScore: number | null;
  estimatedMonthsToRace: number | null;
  assessmentDate: string;
  /** null at intake (CHECK chk_rra_valid_until). */
  validUntil: string | null;
  remarks: string | null;
  trainerId: number | null;
  /** NOT NULL = intake evaluation. null = periodic evaluation. */
  admissionId: number | null;
  createdAt: string;
}

export interface HorseSummary {
  id: number;
  name: string;
  breed: string | null;
  dateOfBirth: string | null;
  currentStatus: string;
  currentStallId: number | null;
  registryName: string | null;
  registrationNumber: string | null;
}

export interface TrainerAdmissionView {
  admission: AdmissionDetailResponse;
  /** null if the Groom step has not created the Horse record. The evaluation CANNOT be submitted then. */
  horse: HorseSummary | null;
  /** The vet's examination data. */
  healthRecords: HealthRecord[];
  /** Vital signs data (temperature, heart rate, respiratory rate...). */
  healthMetrics: HorseHealthMetricResponse[];
  /** Non-null = already evaluated -> the form becomes read-only. */
  existingAssessment: RacingReadinessAssessment | null;
}

export interface TrainerReviewRequest {
  readinessStatus: RacingReadinessStatus;
  conformationScore?: number | null;
  temperamentScore?: number | null;
  gaitQualityScore?: number | null;
  estimatedMonthsToRace?: number | null;
  remarks?: string | null;
}
