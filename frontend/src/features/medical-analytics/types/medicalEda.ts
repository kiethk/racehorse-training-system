export type BioMetricType = 'TEMPERATURE' | 'RESTING_HEART_RATE';

export interface BioSpcPoint {
  id: number;
  recordedAt: string;
  value: number;
  isSpecialCause: boolean;
  anomalyDescription: string | null;
}

export interface BioSpcSeriesResponse {
  horseId: number;
  horseName: string;
  metricType: BioMetricType;
  baselineMean: number | null;
  baselineStdDev: number | null;
  sampleCount: number;
  isBaselineActive: boolean;
  upperControlLimit: number | null;
  lowerControlLimit: number | null;
  upperWarningLimit: number | null;
  lowerWarningLimit: number | null;
  speciesMinLimit: number;
  speciesMaxLimit: number;
  points: BioSpcPoint[];
}

export type SurvivalStratifyBy = 'TREATMENT_PLAN' | 'BODY_ZONE';

export interface KaplanMeierStep {
  days: number;
  survivalProbability: number;
  atRiskCount: number;
  recoveredEventsCount: number;
}

export interface SurvivalStratum {
  stratumId: number;
  stratumName: string;
  totalCases: number;
  recoveredCount: number;
  censoredCount: number;
  medianRecoveryDays: number | null;
  steps: KaplanMeierStep[];
}

export interface SurvivalEdaResponse {
  stratifyBy: SurvivalStratifyBy;
  strata: SurvivalStratum[];
}
