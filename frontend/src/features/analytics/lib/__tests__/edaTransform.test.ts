import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { formatCorrelationStrength, calculateTrendLinePoints } from '../edaTransform.ts';

describe('EDA Transform & Helper Unit Tests', () => {
  it('formatCorrelationStrength maps STRONG_POSITIVE correctly', () => {
    const res = formatCorrelationStrength('STRONG_POSITIVE', 0.85);
    assert.equal(res.tone, 'info');
    assert.match(res.label, /\+0\.85/);
    assert.match(res.label, /Strong Positive/);
    assert.equal(res.qualitativeLabel, 'Strong Positive');
    assert.equal(res.tooltip, 'Pearson r = +0.85');
  });

  it('formatCorrelationStrength maps STRONG_NEGATIVE as success', () => {
    const res = formatCorrelationStrength('STRONG_NEGATIVE', -0.72);
    assert.equal(res.tone, 'success');
    assert.match(res.label, /-0\.72/);
    assert.match(res.label, /Strong Negative/);
    assert.equal(res.qualitativeLabel, 'Strong Negative');
    assert.equal(res.tooltip, 'Pearson r = -0.72');
  });

  it('formatCorrelationStrength handles INSUFFICIENT_DATA and NO_VARIANCE', () => {
    const insufficient = formatCorrelationStrength('INSUFFICIENT_DATA', 0);
    assert.equal(insufficient.tone, 'warning');
    assert.equal(insufficient.label, 'Insufficient Data');
    assert.equal(insufficient.qualitativeLabel, 'Insufficient Data');
    assert.equal(insufficient.tooltip, 'Pearson r = N/A (insufficient data)');

    const noVar = formatCorrelationStrength('NO_VARIANCE', 0);
    assert.equal(noVar.tone, 'neutral');
    assert.equal(noVar.label, 'Zero / Insufficient Variance');
    assert.equal(noVar.qualitativeLabel, 'Zero / Insufficient variance');
    assert.equal(noVar.tooltip, 'Pearson r = N/A (zero variance)');
  });

  it('calculateTrendLinePoints generates endpoints for regression line', () => {
    // y = 2x + 10, minX = 20, maxX = 60
    const points = calculateTrendLinePoints(20, 60, 2, 10);
    assert.equal(points.length, 2);
    assert.deepEqual(points[0], { x: 20, y: 50 });
    assert.deepEqual(points[1], { x: 60, y: 130 });
  });

  it('calculateTrendLinePoints returns empty array if minX >= maxX', () => {
    const points = calculateTrendLinePoints(50, 50, 2, 10);
    assert.equal(points.length, 0);
  });
});
