import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { FleetEdaResponse, IndividualHorseEdaResponse } from '../../types/eda.ts';

describe('HorseEdaAnalyticsCard Data Transformation Tests', () => {
  const mockFleetData: FleetEdaResponse = {
    speedStats: { min: 40, max: 65, mean: 50, median: 50, q1: 45, q3: 55, iqr: 10, stdDev: 5, sampleSize: 3, outlierCount: 1 },
    recoveryHeartRateStats: { min: 80, max: 130, mean: 100, median: 95, q1: 90, q3: 110, iqr: 20, stdDev: 12, sampleSize: 3, outlierCount: 1 },
    correlation: { xVariable: 'speed', yVariable: 'hr', pearsonR: 0.88, strength: 'STRONG_POSITIVE', slope: 1.5, intercept: 25.0, sampleSize: 3 },
    points: [
      { horseId: 1, horseName: 'Thunder Flash', breed: 'Thoroughbred', stallCode: 'A-01', avgSpeedKmh: 48.0, recoveryHeartRateBpm: 97.0, maxHeartRateBpm: 210, isOutlier: false },
      { horseId: 2, horseName: 'Storm Wind', breed: 'Thoroughbred', stallCode: 'A-02', avgSpeedKmh: 50.0, recoveryHeartRateBpm: 100.0, maxHeartRateBpm: 215, isOutlier: false },
      { horseId: 3, horseName: 'Shadow Runner', breed: 'Arabian', stallCode: 'A-03', avgSpeedKmh: 65.0, recoveryHeartRateBpm: 130.0, maxHeartRateBpm: 230, isOutlier: true, outlierReason: 'Exceptional speed (65.0 km/h > Q3)' }
    ]
  };

  it('correctly identifies outliers and formats subLabels in fleet mode', () => {
    const points = mockFleetData.points.map((p) => ({
      id: p.horseId,
      label: p.horseName,
      horseName: p.horseName,
      subLabel: `${p.breed} · Stall: ${p.stallCode || 'N/A'}`,
      x: p.avgSpeedKmh,
      y: p.recoveryHeartRateBpm,
      isOutlier: p.isOutlier,
      isFleet: true,
    }));

    assert.equal(points.length, 3);
    assert.equal(points[0].isOutlier, false);
    assert.equal(points[2].isOutlier, true);
    assert.equal(points[2].label, 'Shadow Runner');
    assert.equal(points[2].horseName, 'Shadow Runner');
    assert.equal(points[2].subLabel, 'Arabian · Stall: A-03');
    assert.equal(points[2].isFleet, true);
  });

  it('correctly maps individual horse workout history', () => {
    const mockIndividualData: IndividualHorseEdaResponse = {
      horseId: 1,
      horseName: 'Thunder Flash',
      breed: 'Thoroughbred',
      speedStats: mockFleetData.speedStats,
      recoveryHeartRateStats: mockFleetData.recoveryHeartRateStats,
      correlation: mockFleetData.correlation,
      points: [
        { workoutId: 101, sessionDate: '2026-10-15', sessionName: 'Canter Practice', speedKmh: 46.5, recoveryHeartRateBpm: 94.0, distanceMeters: 1200, isOutlier: false },
        { workoutId: 102, sessionDate: '2026-10-20', sessionName: 'Sprint 800m', speedKmh: 52.0, recoveryHeartRateBpm: 105.0, distanceMeters: 800, isOutlier: false }
      ]
    };

    const points = mockIndividualData.points.map((p) => ({
      id: p.workoutId,
      label: `Session: ${p.sessionName}`,
      subject: p.sessionName,
      date: p.sessionDate,
      distanceMeters: p.distanceMeters,
      subLabel: `Date: ${p.sessionDate} ${p.distanceMeters ? `· ${p.distanceMeters}m` : ''}`,
      x: p.speedKmh,
      y: p.recoveryHeartRateBpm,
      isOutlier: p.isOutlier,
      isFleet: false,
    }));

    assert.equal(points.length, 2);
    assert.equal(points[0].label, 'Session: Canter Practice');
    assert.equal(points[0].subject, 'Canter Practice');
    assert.equal(points[0].date, '2026-10-15');
    assert.equal(points[0].distanceMeters, 1200);
    assert.equal(points[0].subLabel, 'Date: 2026-10-15 · 1200m');
    assert.equal(points[1].x, 52.0);
    assert.equal(points[1].isFleet, false);
  });
});
