import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import type { CareType } from '../../../types/index.ts';
import type { ExamVariant } from '../examVariantConfig.ts';
import { applyMarkerCoupling, markerCouplingEnabled } from '../decisionCoupling.ts';

describe('markerCouplingEnabled', () => {
  it('applies to the standard INITIAL exam', () => {
    assert.equal(markerCouplingEnabled('STANDARD' as ExamVariant, 'INITIAL' as CareType), true);
  });

  it('applies to the standard ROUTINE exam', () => {
    assert.equal(markerCouplingEnabled('STANDARD' as ExamVariant, 'ROUTINE' as CareType), true);
  });

  it('never applies to the urgent variant', () => {
    assert.equal(markerCouplingEnabled('URGENT' as ExamVariant, 'URGENT' as CareType), false);
    assert.equal(markerCouplingEnabled('URGENT_LEGACY' as ExamVariant, 'INITIAL' as CareType), false);
  });
});

describe('applyMarkerCoupling', () => {
  it('placing a marker while ALLOWED switches the decision to BLOCKED', () => {
    const result = applyMarkerCoupling(0, 1, 'ALLOWED', false);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, true);
    assert.equal(result.autoSwitch, 'TO_BLOCKED');
  });

  it('placing a marker while already BLOCKED manually keeps the manual block', () => {
    const result = applyMarkerCoupling(0, 1, 'BLOCKED', false);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, false);
    assert.equal(result.autoSwitch, null);
  });

  it('removing the last marker reverts a marker-caused block to ALLOWED', () => {
    const result = applyMarkerCoupling(1, 0, 'BLOCKED', true);
    assert.equal(result.decision, 'ALLOWED');
    assert.equal(result.blockedByMarker, false);
    assert.equal(result.autoSwitch, 'TO_ALLOWED');
  });

  it('removing the last marker keeps a manually chosen BLOCKED decision', () => {
    const result = applyMarkerCoupling(1, 0, 'BLOCKED', false);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, false);
    assert.equal(result.autoSwitch, null);
  });

  it('removing one of several markers keeps the block', () => {
    const result = applyMarkerCoupling(2, 1, 'BLOCKED', true);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, true);
    assert.equal(result.autoSwitch, null);
  });

  it('adding a marker while BLOCKED by marker keeps the state', () => {
    const result = applyMarkerCoupling(1, 2, 'BLOCKED', true);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, true);
    assert.equal(result.autoSwitch, null);
  });
});
