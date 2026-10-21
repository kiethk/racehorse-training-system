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
}

export interface CorrectInjuryRequest {
  correctionReason: string;
  injuryType?: string;
  severity?: string;
  description?: string;
  status?: string;
}
