import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  EXAM_VARIANT_CONFIG,
  followUpMerge,
  sixVitalsRequired,
  validateSixVitals,
  tier1VerdictEnabled,
} from '../examVariantConfig.ts';

/**
 * Pins the behavioral contract of each exam variant. If someone flips a knob,
 * the flow it describes changes for vets — these assertions fail first.
 */
describe('examVariantConfig — per-flow behavioral contract', () => {
  const standard = EXAM_VARIANT_CONFIG.STANDARD;
  const legacy = EXAM_VARIANT_CONFIG.URGENT_LEGACY;
  const urgent = EXAM_VARIANT_CONFIG.URGENT;

  it('admission/routine exam keeps structured findings, draft, confirm dialog, vaccination', () => {
    assert.equal(standard.section2Mode, 'structured');
    assert.equal(standard.sampleFindings, false);
    assert.equal(standard.draft, true);
    assert.equal(standard.confirmDialog, true);
    assert.equal(standard.dirtyGuard, true);
    assert.equal(standard.vaccination, true);
    assert.equal(standard.simulateMode, 'resting-inline');
    assert.equal(standard.submitAdapter, 'admission-aware');
    assert.equal(standard.defaultDecision, 'ALLOWED');
  });

  it('urgent case uses free-text section 2 with samples, no draft/confirm, BLOCKED default', () => {
    assert.equal(urgent.section2Mode, 'freeText');
    assert.equal(urgent.showFindingsTextarea, true);
    assert.equal(urgent.sampleFindings, true);
    assert.equal(urgent.markersAppendToFindings, true);
    assert.equal(urgent.decisionUi, 'inline-cards');
    assert.equal(urgent.defaultDecision, 'BLOCKED');
    assert.equal(urgent.draft, false);
    assert.equal(urgent.confirmDialog, false);
    assert.equal(urgent.vaccination, false);
    assert.equal(urgent.simulateMode, 'hot-profiles');
    assert.equal(urgent.submitAdapter, 'care-schedule');
    assert.equal(urgent.errorMapping, 'raw');
    assert.equal(urgent.submitLabel, 'Complete Urgent Examination');
  });

  it('legacy urgent-in-admission-queue path keeps the VRF chrome (draft, vaccination, single follow-up)', () => {
    assert.equal(legacy.section2Mode, 'freeText');
    assert.equal(legacy.showFindingsTextarea, false);
    assert.equal(legacy.sampleFindings, false);
    assert.equal(legacy.draft, true);
    assert.equal(legacy.vaccination, true);
    assert.equal(legacy.submitAdapter, 'admission-aware');
  });

  it('restriction messages stay distinct per flow (user-visible strings)', () => {
    assert.equal(
      standard.restrictionZodMessage,
      'An assessment note is required when training is blocked.',
    );
    assert.equal(
      urgent.restrictionZodMessage,
      'Restriction details are required when training is blocked.',
    );
    assert.notEqual(standard.restrictionLabel, urgent.restrictionLabel);
  });

  it('layout split: decision panel sticky for admission flow, stacked for urgent', () => {
    assert.equal(standard.layout, 'split-sticky-decision-panel');
    assert.equal(urgent.layout, 'stacked');
    assert.equal(legacy.layout, 'split-sticky-decision-panel');
  });

  it('sixVitalsRequired applies exclusively to INITIAL and ROUTINE FOLLOW_UP', () => {
    assert.equal(sixVitalsRequired('STANDARD', 'INITIAL'), true);
    assert.equal(sixVitalsRequired('STANDARD', 'ROUTINE', 'FOLLOW_UP'), true);
    assert.equal(sixVitalsRequired('STANDARD', 'ROUTINE', 'FOLLOW_UP', true), false);
    assert.equal(sixVitalsRequired('STANDARD', 'ROUTINE', 'PREVENTIVE'), false);
    assert.equal(sixVitalsRequired('URGENT', 'URGENT'), false);
    assert.equal(sixVitalsRequired('URGENT_LEGACY', 'INITIAL'), false);
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

  it('tier1VerdictEnabled activates only for STANDARD INITIAL admissions', () => {
    assert.equal(tier1VerdictEnabled('STANDARD', 'INITIAL'), true);
    assert.equal(tier1VerdictEnabled('STANDARD', 'ROUTINE'), false);
    assert.equal(tier1VerdictEnabled('URGENT', 'URGENT'), false);
  });
});
