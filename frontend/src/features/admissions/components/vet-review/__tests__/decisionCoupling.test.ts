import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import type { CareType, TrainingDecision } from '../../../types/index.ts';
import {
  couplingDecision,
  effectiveMarkerCount,
  markerCouplingEnabled,
} from '../decisionCoupling.ts';

describe('markerCouplingEnabled', () => {
  it('applies to the INITIAL exam', () => {
    assert.equal(markerCouplingEnabled('INITIAL' as CareType), true);
  });

  it('applies to the ROUTINE follow-up exam', () => {
    assert.equal(markerCouplingEnabled('ROUTINE' as CareType, 'FOLLOW_UP'), true);
  });

  it('never applies to the urgent exam', () => {
    assert.equal(markerCouplingEnabled('URGENT' as CareType), false);
  });

  it('never applies to a preventive (vaccination) visit', () => {
    assert.equal(markerCouplingEnabled('ROUTINE' as CareType, 'PREVENTIVE'), false);
  });
});

describe('effectiveMarkerCount', () => {
  it('counts active conditions not yet marked recovered', () => {
    assert.equal(effectiveMarkerCount([{ id: 4 }, { id: 5 }], [], []), 2);
  });

  it('drops a condition once the Vet marks it recovered', () => {
    assert.equal(effectiveMarkerCount([{ id: 4 }, { id: 5 }], [4], []), 1);
  });

  it('adds markers placed in this session', () => {
    assert.equal(effectiveMarkerCount([{ id: 4 }], [], ['new-marker']), 2);
  });

  it('returns zero when nothing is left', () => {
    assert.equal(effectiveMarkerCount([{ id: 4 }], [4], []), 0);
  });
});

describe('couplingDecision', () => {
  it('blocks training when the follow-up still has an active condition', () => {
    const result = couplingDecision(1, 'ALLOWED' as TrainingDecision, false);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, true);
  });

  it('keeps the block while one of several conditions is still active', () => {
    const result = couplingDecision(1, 'BLOCKED' as TrainingDecision, true);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, true);
  });

  it('allows training once the last condition is resolved', () => {
    const result = couplingDecision(0, 'BLOCKED' as TrainingDecision, true);
    assert.equal(result.decision, 'ALLOWED');
    assert.equal(result.blockedByMarker, false);
  });

  it('keeps a manually chosen BLOCKED decision', () => {
    const result = couplingDecision(0, 'BLOCKED' as TrainingDecision, false);
    assert.equal(result.decision, 'BLOCKED');
    assert.equal(result.blockedByMarker, false);
  });

  it('leaves an allowed horse with no markers alone', () => {
    const result = couplingDecision(0, 'ALLOWED' as TrainingDecision, false);
    assert.equal(result.decision, 'ALLOWED');
    assert.equal(result.blockedByMarker, false);
  });
});
