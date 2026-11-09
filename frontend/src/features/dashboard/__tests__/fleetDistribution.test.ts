import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { DashboardHorse, FleetSummary } from '../types';

describe('FleetDistribution & Specialty Calculations', () => {
  it('computes ready/idle horses accurately without double counting inTraining or medicalBlocked', () => {
    const summary: FleetSummary = {
      totalHorses: 42,
      activeEligible: 42,
      inTraining: 29,
      medicalBlocked: 5,
      inactive: 0,
    };

    const readyHorses = Math.max(0, summary.activeEligible - summary.inTraining - summary.medicalBlocked);
    assert.equal(readyHorses, 8);

    const segmentSum = summary.inTraining + readyHorses + summary.medicalBlocked + summary.inactive;
    assert.equal(segmentSum, summary.totalHorses);
  });

  it('aggregates horse distance specialties correctly into bands and developing', () => {
    const mockHorses: Partial<DashboardHorse>[] = [
      {
        horseId: 1,
        specialty: {
          minWorkoutsPerGroup: 3,
          strongDistances: ['SPRINT'],
          preferredSurface: null,
          distanceStats: [],
        },
      },
      {
        horseId: 2,
        specialty: {
          minWorkoutsPerGroup: 3,
          strongDistances: ['MILE'],
          preferredSurface: null,
          distanceStats: [],
        },
      },
      {
        horseId: 3,
        specialty: {
          minWorkoutsPerGroup: 3,
          strongDistances: ['MIDDLE', 'STAYER'],
          preferredSurface: null,
          distanceStats: [
            { band: 'MIDDLE', workoutCount: 4, averageRating: 7.5, averageTopSpeedKmh: 60 },
            { band: 'STAYER', workoutCount: 4, averageRating: 8.8, averageTopSpeedKmh: 58 },
          ],
        },
      },
      {
        horseId: 4,
        specialty: {
          minWorkoutsPerGroup: 3,
          strongDistances: [],
          preferredSurface: null,
          distanceStats: [],
        },
      },
    ];

    const counts = {
      SPRINT: 0,
      MILE: 0,
      MIDDLE: 0,
      STAYER: 0,
      DEVELOPING: 0,
    };

    for (const h of mockHorses) {
      const strong = h.specialty?.strongDistances ?? [];
      if (strong.length === 0) {
        counts.DEVELOPING += 1;
      } else if (strong.length === 1) {
        counts[strong[0] as keyof typeof counts] += 1;
      } else {
        const stats = h.specialty?.distanceStats ?? [];
        const sorted = [...strong].sort((a, b) => {
          const statA = stats.find((s) => s.band === a)?.averageRating ?? 0;
          const statB = stats.find((s) => s.band === b)?.averageRating ?? 0;
          return statB - statA;
        });
        counts[sorted[0] as keyof typeof counts] += 1;
      }
    }

    assert.equal(counts.SPRINT, 1);
    assert.equal(counts.MILE, 1);
    assert.equal(counts.MIDDLE, 0); // Stayer had higher rating (8.8 vs 7.5)
    assert.equal(counts.STAYER, 1);
    assert.equal(counts.DEVELOPING, 1);
    assert.equal(
      counts.SPRINT + counts.MILE + counts.MIDDLE + counts.STAYER + counts.DEVELOPING,
      mockHorses.length
    );
  });
});
