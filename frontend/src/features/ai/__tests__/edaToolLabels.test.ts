import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { getEdaToolLabel } from '../lib/edaToolLabels.ts';

describe('getEdaToolLabel', () => {
  it('returns the exact English label for each supported EDA analysis mode', () => {
    const labels = {
      FLEET_PERFORMANCE: 'Analyze fleet EDA statistics',
      HORSE_PERFORMANCE: 'Analyze horse EDA statistics',
      BIO_SPC: 'Generate Bio-SPC control chart',
      SURVIVAL: 'Compute Kaplan-Meier survival analysis',
      MANAGER_OVERVIEW: 'Synthesize manager EDA overview',
      ADMISSION_FUNNEL: 'Analyze admission funnel',
      TRAINER_PERFORMANCE: 'Evaluate trainer performance',
    };

    for (const [analysisType, expectedLabel] of Object.entries(labels)) {
      assert.equal(getEdaToolLabel(analysisType), expectedLabel);
    }
  });

  it('returns null for unknown and missing analysis modes', () => {
    assert.equal(getEdaToolLabel('UNKNOWN_MODE'), null);
    assert.equal(getEdaToolLabel('constructor'), null);
    assert.equal(getEdaToolLabel(undefined), null);
    assert.equal(getEdaToolLabel({}), null);
  });
});
