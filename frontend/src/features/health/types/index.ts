export interface InjuryRecord {
  id: number;
  horseId: number;
  healthRecordId: number;
  injuryType: string;
  bodyRegionId: number;
  severity: string;
  description?: string;
  diagnosedAt: string;
  expectedRecoveryDate?: string;
  recoveredAt?: string;
  status: string;
  positionX?: number | null;
  positionY?: number | null;
  positionZ?: number | null;
  zoneId?: string | null;
  localX?: number | null;
  localY?: number | null;
  localZ?: number | null;
  normalX?: number | null;
  normalY?: number | null;
  normalZ?: number | null;
  meshName?: string | null;
}

export interface HorseHealthSummary {
  horseId: number;
  horseName: string;
  trainingDecision: 'ALLOWED' | 'BLOCKED';
  trainingDecisionReason?: string;
  hasPendingUrgent: boolean;
  lastExaminedAt?: string;
  lastDiagnosis?: string;
  activeInjuryCount: number;
  nextScheduledCare?: string;
  /** Open urgent case of this horse (reported but not yet examined), if any. */
  pendingUrgentScheduleId?: number | null;
  /** Who reported the incident behind the pending urgent case. */
  pendingUrgentReportedByName?: string | null;
  /** Which vet the pending urgent case is assigned to. */
  pendingUrgentVetId?: number | null;
  pendingUrgentVetName?: string | null;
  /** A block coming from the admission intake (quarantine), with its submitting owner. */
  pendingAdmissionId?: number | null;
  pendingAdmissionOwnerName?: string | null;
}

export interface CorrectInjuryRequest {
  correctionReason: string;
  injuryType?: string;
  severity?: string;
  description?: string;
  status?: string;
  /** New 3D marker placement captured on the model — optional reposition. */
  marker?: {
    positionX: number;
    positionY: number;
    positionZ: number;
    zoneId: string;
    normalX?: number;
    normalY?: number;
    normalZ?: number;
  };
}

/** Mirrors the backend HealthRecord entity — one entry of a horse's health history. */
export interface HealthRecord {
  id: number;
  horseId: number;
  veterinarianId: number;
  careScheduleId: number | null;
  sourceTrainingWorkoutId: number | null;
  examinedAt: string;
  symptoms: string | null;
  findings: string | null;
  diagnosis: string | null;
  treatment: string | null;
  trainingDecision: 'ALLOWED' | 'BLOCKED' | null;
  restrictionDetails: string | null;
  recordType: string;
  productOrService: string | null;
  notes: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}
