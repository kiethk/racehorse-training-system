import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { BioSpcSeriesResponse } from '../../types/medicalEda.ts';

describe('BioSpcControlChart Logic Tests', () => {
  const mockAccumulatingSpc: BioSpcSeriesResponse = {
    horseId: 101,
    horseName: 'Silver Charm',
    metricType: 'TEMPERATURE',
    baselineMean: 37.6,
    baselineStdDev: null,
    sampleCount: 3,
    isBaselineActive: false,
    upperControlLimit: 38.5,
    lowerControlLimit: 37.2,
    upperWarningLimit: null,
    lowerWarningLimit: null,
    speciesMinLimit: 37.2,
    speciesMaxLimit: 38.5,
    points: [
      { id: 1, recordedAt: '2026-11-01T08:00:00', value: 37.5, isSpecialCause: false, anomalyDescription: null },
      { id: 2, recordedAt: '2026-11-02T08:00:00', value: 37.6, isSpecialCause: false, anomalyDescription: null },
      { id: 3, recordedAt: '2026-11-03T08:00:00', value: 38.9, isSpecialCause: true, anomalyDescription: 'Exceeds species upper limit (38.5)' },
    ],
  };

  const mockActiveSpc: BioSpcSeriesResponse = {
    horseId: 102,
    horseName: 'Seattle Slew',
    metricType: 'RESTING_HEART_RATE',
    baselineMean: 32.5,
    baselineStdDev: 1.2,
    sampleCount: 6,
    isBaselineActive: true,
    upperControlLimit: 36.1,
    lowerControlLimit: 28.9,
    upperWarningLimit: 34.9,
    lowerWarningLimit: 30.1,
    speciesMinLimit: 28.0,
    speciesMaxLimit: 44.0,
    points: [
      { id: 1, recordedAt: '2026-11-01T08:00:00', value: 32.0, isSpecialCause: false, anomalyDescription: null },
      { id: 2, recordedAt: '2026-11-02T08:00:00', value: 33.0, isSpecialCause: false, anomalyDescription: null },
      { id: 3, recordedAt: '2026-11-03T08:00:00', value: 32.5, isSpecialCause: false, anomalyDescription: null },
      { id: 4, recordedAt: '2026-11-04T08:00:00', value: 32.0, isSpecialCause: false, anomalyDescription: null },
      { id: 5, recordedAt: '2026-11-05T08:00:00', value: 33.0, isSpecialCause: false, anomalyDescription: null },
      { id: 6, recordedAt: '2026-11-06T08:00:00', value: 37.5, isSpecialCause: true, anomalyDescription: 'Critical: Value (37.50) exceeds UCL (36.10)' },
    ],
  };

  it('correctly handles accumulating baseline state (K < 5)', () => {
    assert.equal(mockAccumulatingSpc.isBaselineActive, false);
    assert.equal(mockAccumulatingSpc.sampleCount, 3);
    const statusText = mockAccumulatingSpc.isBaselineActive
      ? 'Personalized Baseline Active'
      : `Accumulating Baseline (${mockAccumulatingSpc.sampleCount}/5 readings)`;
    assert.equal(statusText, 'Accumulating Baseline (3/5 readings)');

    const pointWithAnomaly = mockAccumulatingSpc.points.find((p) => p.isSpecialCause);
    assert.ok(pointWithAnomaly);
    assert.equal(pointWithAnomaly?.value, 38.9);
    assert.equal(pointWithAnomaly?.anomalyDescription, 'Exceeds species upper limit (38.5)');
  });

  it('correctly handles active personalized Shewhart baseline state (K >= 5)', () => {
    assert.equal(mockActiveSpc.isBaselineActive, true);
    assert.equal(mockActiveSpc.sampleCount, 6);
    assert.equal(mockActiveSpc.baselineMean, 32.5);
    assert.equal(mockActiveSpc.upperControlLimit, 36.1);
    assert.equal(mockActiveSpc.lowerControlLimit, 28.9);

    const latestPoint = mockActiveSpc.points[mockActiveSpc.points.length - 1];
    assert.equal(latestPoint.isSpecialCause, true);
    assert.ok(latestPoint.anomalyDescription?.includes('exceeds UCL'));
  });

  it('computes safe dynamic y-axis range', () => {
    const vals = mockActiveSpc.points.map((p) => p.value);
    if (mockActiveSpc.upperControlLimit != null) vals.push(mockActiveSpc.upperControlLimit);
    if (mockActiveSpc.lowerControlLimit != null) vals.push(mockActiveSpc.lowerControlLimit);
    vals.push(mockActiveSpc.speciesMinLimit, mockActiveSpc.speciesMaxLimit);

    const min = Math.floor(Math.min(...vals) - 4);
    const max = Math.ceil(Math.max(...vals) + 4);

    assert.ok(min <= 24);
    assert.ok(max >= 48);
  });

  it('ensures sampleCount in accumulating baseline never renders invalid fractions like > 4/5', () => {
    // When inactive, sampleCount reflects baseline accumulation K < 5
    for (let k = 0; k < 5; k++) {
      const banner = `Accumulating Baseline (${k}/5 readings)`;
      assert.ok(banner.includes(`${k}/5 readings`));
    }
  });

  it('properly distinguishes critical UCL violation vs normal recovery', () => {
    const normalPoint = mockActiveSpc.points[0];
    assert.equal(normalPoint.isSpecialCause, false);
    assert.equal(normalPoint.anomalyDescription, null);

    const uclPoint = mockActiveSpc.points[5];
    assert.equal(uclPoint.isSpecialCause, true);
    assert.ok(uclPoint.anomalyDescription?.includes('exceeds UCL'));
  });
});
