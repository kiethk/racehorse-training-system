export interface VeterinarianDelayDto {
  veterinarianId: number;
  veterinarianName: string;
  totalSchedules: number;
  totalRollovers: number;
  rolloverRate: number;
  zScore: number;
  individualDelinquencyAlert: boolean;
}

export interface RescheduleReasonBreakdownDto {
  reason: string;
  count: number;
  percentage: number;
}

export interface VetDelaysEdaDto {
  clinicRolloverRate: number;
  totalCareSchedules: number;
  totalRolloverEvents: number;
  systemicUnderstaffingAlert: boolean;
  diagnosticSummary: string;
  reasonBreakdown: RescheduleReasonBreakdownDto[];
  veterinarians: VeterinarianDelayDto[];
}

export interface TrainerPerformanceDto {
  trainerId: number;
  trainerName: string;
  scheduledWorkouts: number;
  completedWorkouts: number;
  heldWorkouts: number;
  cancelledWorkouts: number;
  adherenceRate: number;
  averageRating: number;
  averageSpeedKmh: number;
  averageRecoveryHeartRate: number;
  speedGainKmh: number;
  hrRecoveryImprovementBpm: number;
}

export interface TrainerEvaluationEdaDto {
  overallAdherenceRate: number;
  totalWorkoutsScheduled: number;
  totalWorkoutsCompleted: number;
  trainers: TrainerPerformanceDto[];
}

export interface BarnOccupancyBreakdownDto {
  areaId: number;
  areaCode: string;
  areaName: string;
  areaType: string;
  totalStalls: number;
  occupiedStalls: number;
  availableStalls: number;
  occupancyRate: number;
}

export interface StallOccupancyEdaDto {
  overallRate: number;
  totalStalls: number;
  occupiedStalls: number;
  availableStalls: number;
  quarantineTotal: number;
  quarantineOccupied: number;
  quarantineAvailable: number;
  quarantineRate: number;
  regularTotal: number;
  regularOccupied: number;
  regularAvailable: number;
  regularRate: number;
  awaitingArrivalAdmissions: number;
  inflowBufferMargin: number;
  quarantineStressAlert: boolean;
  barnBreakdown: BarnOccupancyBreakdownDto[];
}

export interface AdmissionStageLatenciesDto {
  arrivalLeadTimeHours: number;
  groomReviewHours: number;
  vetReviewHours: number;
  trainerReviewHours: number;
  managerReviewHours: number;
  totalEndToEndHours: number;
}

export interface StageFunnelPointDto {
  stage: string;
  count: number;
  conversionPercentage: number;
}

export interface AdmissionFunnelEdaDto {
  totalApplications: number;
  approvedCount: number;
  rejectedCount: number;
  expiredCount: number;
  inProgressCount: number;
  approvalRate: number;
  rejectionRate: number;
  bottleneckStage: 'ARRIVAL' | 'GROOM_REVIEW' | 'VET_REVIEW' | 'TRAINER_REVIEW' | 'MANAGER_REVIEW' | 'NONE' | string;
  medianLatenciesHours: AdmissionStageLatenciesDto;
  stageFunnel: StageFunnelPointDto[];
}

export interface ManagerEdaOverviewResponse {
  timeWindowDays: number;
  vetDelays: VetDelaysEdaDto;
  trainerEvaluation: TrainerEvaluationEdaDto;
  stallOccupancy: StallOccupancyEdaDto;
  admissionFunnel: AdmissionFunnelEdaDto;
}
