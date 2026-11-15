import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  EXAM_CONFIG,
  followUpMerge,
  sixVitalsRequired,
  validateSixVitals,
  tier1VerdictEnabled,
} from '../examVariantConfig.ts';

/**
 * Pins the behavioural contract of the single exam form. If someone flips a knob,
 * the flow it describes changes for vets — these assertions fail first.
 */
describe('EXAM_CONFIG — behavioural contract', () => {
  it('keeps structured findings, draft, confirm dialog and vaccination', () => {
    assert.equal(EXAM_CONFIG.section2Mode, 'structured');
    assert.equal(EXAM_CONFIG.sampleFindings, false);
    assert.equal(EXAM_CONFIG.draft, true);
    assert.equal(EXAM_CONFIG.confirmDialog, true);
    assert.equal(EXAM_CONFIG.dirtyGuard, true);
    assert.equal(EXAM_CONFIG.vaccination, true);
    assert.equal(EXAM_CONFIG.simulateMode, 'resting-inline');
    assert.equal(EXAM_CONFIG.submitAdapter, 'admission-aware');
    assert.equal(EXAM_CONFIG.defaultDecision, 'ALLOWED');
    assert.equal(EXAM_CONFIG.layout, 'split-sticky-decision-panel');
  });

  it('sixVitalsRequired applies exclusively to INITIAL and ROUTINE FOLLOW_UP', () => {
    assert.equal(sixVitalsRequired('INITIAL'), true);
    assert.equal(sixVitalsRequired('ROUTINE', 'FOLLOW_UP'), true);
    assert.equal(sixVitalsRequired('ROUTINE', 'FOLLOW_UP', true), false);
    assert.equal(sixVitalsRequired('ROUTINE', 'PREVENTIVE'), false);
    assert.equal(sixVitalsRequired('URGENT'), false);
  });

  it('validateSixVitals verifies presence and spec ranges', () => {
    const valid = {
      temperature: '37.8',
      heartRate: '36',
      respiratoryRate: '12',
      weight: '500',
      bodyConditionScore: '5.5',
      hydrationStatus: 'NORMAL',
    };
    assert.deepEqual(validateSixVitals(valid), {});

    const missingAll = validateSixVitals({});
    assert.equal(Object.keys(missingAll).length, 6);

    // A sick horse is still recordable; only impossible readings are refused.
    const feverish = validateSixVitals({ ...valid, temperature: '39.2', heartRate: '50', respiratoryRate: '20' });
    assert.deepEqual(feverish, {});

    const outOfRange = validateSixVitals({
      temperature: '50',
      heartRate: '300',
      respiratoryRate: '150',
      weight: '1600',
      bodyConditionScore: '0',
      hydrationStatus: 'NORMAL',
    });
    assert.equal(Object.keys(outOfRange).length, 5);
    assert.ok(outOfRange.temperature);
    assert.ok(outOfRange.heartRate);
    assert.ok(outOfRange.respiratoryRate);
    assert.ok(outOfRange.weight);
    assert.ok(outOfRange.bodyConditionScore);
  });

  it('followUpMerge: an earlier date moves the waiting Follow-up, a later date keeps it', () => {
    assert.equal(followUpMerge('2026-11-06', '2026-11-07T14:00:00'), 'MOVED');
    assert.equal(followUpMerge('2026-11-11', '2026-11-07T14:00:00'), 'KEPT');
    assert.equal(followUpMerge('2026-11-07', '2026-11-07T14:00:00'), 'KEPT');
    assert.equal(followUpMerge('2026-11-11', null), null);
    assert.equal(followUpMerge('', '2026-11-07T14:00:00'), null);
  });

  it('tier1VerdictEnabled activates only for INITIAL admissions', () => {
    assert.equal(tier1VerdictEnabled('INITIAL'), true);
    assert.equal(tier1VerdictEnabled('ROUTINE'), false);
    assert.equal(tier1VerdictEnabled('URGENT'), false);
  });
});
