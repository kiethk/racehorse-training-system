export interface EdaDistributionStats {
  min: number;
  max: number;
  mean: number;
  median: number;
  q1: number;
  q3: number;
  iqr: number;
  stdDev: number;
  sampleSize: number;
  outlierCount: number;
}

export type CorrelationStrength =
  | 'STRONG_POSITIVE'
  | 'MODERATE_POSITIVE'
  | 'WEAK'
  | 'MODERATE_NEGATIVE'
  | 'STRONG_NEGATIVE'
  | 'NO_VARIANCE'
  | 'INSUFFICIENT_DATA';

export interface EdaCorrelationSummary {
  xVariable: string;
  yVariable: string;
  pearsonR: number;
  strength: CorrelationStrength;
  slope: number;
  intercept: number;
  sampleSize: number;
}

export interface FleetHorseEdaPoint {
  horseId: number;
  horseName: string;
  breed: string;
  stallCode?: string | null;
  trainerName?: string | null;
  avgSpeedKmh: number;
  recoveryHeartRateBpm: number;
  maxHeartRateBpm?: number | null;
  isOutlier: boolean;
  outlierReason?: string | null;
}

export interface IndividualWorkoutEdaPoint {
  workoutId: number;
  sessionDate: string;
  sessionName: string;
  speedKmh: number;
  recoveryHeartRateBpm: number;
  maxHeartRateBpm?: number | null;
  distanceMeters?: number | null;
  isOutlier: boolean;
  outlierReason?: string | null;
}

export interface FleetEdaResponse {
  speedStats: EdaDistributionStats;
  recoveryHeartRateStats: EdaDistributionStats;
  correlation: EdaCorrelationSummary;
  points: FleetHorseEdaPoint[];
}

export interface IndividualHorseEdaResponse {
  horseId: number;
  horseName: string;
  breed: string;
  speedStats: EdaDistributionStats;
  recoveryHeartRateStats: EdaDistributionStats;
  correlation: EdaCorrelationSummary;
  points: IndividualWorkoutEdaPoint[];
}
