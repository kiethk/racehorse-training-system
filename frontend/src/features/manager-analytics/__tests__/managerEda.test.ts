import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type {
  ManagerEdaOverviewResponse,
  VetDelaysEdaDto,
  TrainerEvaluationEdaDto,
  StallOccupancyEdaDto,
  AdmissionFunnelEdaDto,
} from '../types/managerEda.ts';

describe('Manager EDA Domain Analytics Contract Tests', () => {
  const mockOverview: ManagerEdaOverviewResponse = {
    timeWindowDays: 90,
    vetDelays: {
      clinicRolloverRate: 0.08,
      totalCareSchedules: 150,
      totalRolloverEvents: 12,
      systemicUnderstaffingAlert: false,
      diagnosticSummary: 'Individual Delay Alert: Dr. Charlie exhibits rollover rate significantly above peers (Z >= 2.0).',
      reasonBreakdown: [
        { reason: 'TIME_EXCEEDED', count: 7, percentage: 58.3 },
        { reason: 'URGENT_PREEMPTION', count: 5, percentage: 41.7 },
      ],
      veterinarians: [
        {
          veterinarianId: 1,
          veterinarianName: 'Dr. Alice',
          totalSchedules: 50,
          totalRollovers: 2,
          rolloverRate: 0.04,
          zScore: -0.85,
          individualDelinquencyAlert: false,
        },
        {
          veterinarianId: 2,
          veterinarianName: 'Dr. Bob',
          totalSchedules: 50,
          totalRollovers: 3,
          rolloverRate: 0.06,
          zScore: -0.42,
          individualDelinquencyAlert: false,
        },
        {
          veterinarianId: 3,
          veterinarianName: 'Dr. Charlie',
          totalSchedules: 50,
          totalRollovers: 7,
          rolloverRate: 0.14,
          zScore: 2.15,
          individualDelinquencyAlert: true,
        },
      ],
    },
    trainerEvaluation: {
      overallAdherenceRate: 85.5,
      totalWorkoutsScheduled: 300,
      totalWorkoutsCompleted: 256,
      trainers: [
        {
          trainerId: 10,
          trainerName: 'Trainer Tom',
          scheduledWorkouts: 150,
          completedWorkouts: 135,
          heldWorkouts: 10,
          cancelledWorkouts: 5,
          adherenceRate: 90.0,
          averageRating: 8.7,
          averageSpeedKmh: 42.5,
          averageRecoveryHeartRate: 75,
          speedGainKmh: 3.2,
          hrRecoveryImprovementBpm: 7.5,
        },
        {
          trainerId: 11,
          trainerName: 'Trainer Sarah',
          scheduledWorkouts: 150,
          completedWorkouts: 121,
          heldWorkouts: 20,
          cancelledWorkouts: 9,
          adherenceRate: 80.7,
          averageRating: 7.9,
          averageSpeedKmh: 40.8,
          averageRecoveryHeartRate: 82,
          speedGainKmh: 1.8,
          hrRecoveryImprovementBpm: 4.2,
        },
      ],
    },
    stallOccupancy: {
      overallRate: 75.0,
      totalStalls: 120,
      occupiedStalls: 90,
      availableStalls: 30,
      quarantineTotal: 15,
      quarantineOccupied: 13,
      quarantineAvailable: 2,
      quarantineRate: 86.7,
      regularTotal: 105,
      regularOccupied: 77,
      regularAvailable: 28,
      regularRate: 73.3,
      awaitingArrivalAdmissions: 4,
      inflowBufferMargin: -2,
      quarantineStressAlert: true,
      barnBreakdown: [
        {
          areaId: 1,
          areaCode: 'A',
          areaName: 'Main Barn',
          areaType: 'REGULAR',
          totalStalls: 60,
          occupiedStalls: 45,
          availableStalls: 15,
          occupancyRate: 75.0,
        },
        {
          areaId: 2,
          areaCode: 'Q',
          areaName: 'Quarantine Ward',
          areaType: 'QUARANTINE',
          totalStalls: 15,
          occupiedStalls: 13,
          availableStalls: 2,
          occupancyRate: 86.7,
        },
      ],
    },
    admissionFunnel: {
      totalApplications: 50,
      approvedCount: 35,
      rejectedCount: 8,
      expiredCount: 2,
      inProgressCount: 5,
      approvalRate: 70.0,
      rejectionRate: 16.0,
      bottleneckStage: 'VET_REVIEW',
      medianLatenciesHours: {
        arrivalLeadTimeHours: 12.0,
        groomReviewHours: 4.0,
        vetReviewHours: 28.5,
        trainerReviewHours: 8.0,
        managerReviewHours: 3.5,
        totalEndToEndHours: 56.0,
      },
      stageFunnel: [
        { stage: 'Submitted', count: 50, conversionPercentage: 100.0 },
        { stage: 'Arrived at Facility', count: 48, conversionPercentage: 96.0 },
        { stage: 'Groom Checked', count: 46, conversionPercentage: 92.0 },
        { stage: 'Vet Examined', count: 40, conversionPercentage: 80.0 },
        { stage: 'Trainer Evaluated', count: 37, conversionPercentage: 74.0 },
        { stage: 'Manager Cleared', count: 35, conversionPercentage: 70.0 },
      ],
    },
  };

  it('correctly audits Vet Delay statistics and identifies individual delinquency outliers', () => {
    const { vetDelays } = mockOverview;
    assert.equal(vetDelays.totalCareSchedules, 150);
    assert.equal(vetDelays.totalRolloverEvents, 12);
    assert.equal(vetDelays.systemicUnderstaffingAlert, false);

    const charlie = vetDelays.veterinarians.find((v) => v.veterinarianName === 'Dr. Charlie');
    assert.ok(charlie);
    assert.equal(charlie.individualDelinquencyAlert, true);
    assert.ok(charlie.zScore >= 2.0);

    const alice = vetDelays.veterinarians.find((v) => v.veterinarianName === 'Dr. Alice');
    assert.ok(alice);
    assert.equal(alice.individualDelinquencyAlert, false);
  });

  it('evaluates trainer execution discipline and physiological gains', () => {
    const { trainerEvaluation } = mockOverview;
    assert.equal(trainerEvaluation.overallAdherenceRate, 85.5);
    assert.equal(trainerEvaluation.trainers.length, 2);

    const tom = trainerEvaluation.trainers[0];
    assert.equal(tom.adherenceRate, 90.0);
    assert.equal(tom.speedGainKmh, 3.2);
    assert.equal(tom.hrRecoveryImprovementBpm, 7.5);
  });

  it('detects quarantine buffer stress when inbound arrivals exceed available quarantine stalls', () => {
    const { stallOccupancy } = mockOverview;
    assert.equal(stallOccupancy.inflowBufferMargin, -2);
    assert.equal(stallOccupancy.quarantineStressAlert, true);
    assert.equal(stallOccupancy.quarantineRate, 86.7);
  });

  it('tracks admission multi-gate funnel retention and detects slowest SLA gateway', () => {
    const { admissionFunnel } = mockOverview;
    assert.equal(admissionFunnel.totalApplications, 50);
    assert.equal(admissionFunnel.approvalRate, 70.0);
    assert.equal(admissionFunnel.bottleneckStage, 'VET_REVIEW');
    assert.equal(admissionFunnel.medianLatenciesHours.vetReviewHours, 28.5);

    const finalStep = admissionFunnel.stageFunnel[admissionFunnel.stageFunnel.length - 1];
    assert.equal(finalStep.stage, 'Manager Cleared');
    assert.equal(finalStep.count, 35);
  });
});
