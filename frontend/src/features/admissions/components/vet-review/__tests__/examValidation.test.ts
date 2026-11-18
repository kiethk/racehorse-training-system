import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import type { CareType } from '../../../types/index.ts';
import { EXAM_CONFIG } from '../examVariantConfig.ts';
import { businessDateFromToday } from '../businessDate.ts';
import { validateExam, type ExamValidationInput } from '../examValidation.ts';

function base(overrides: Partial<ExamValidationInput> = {}): ExamValidationInput {
  return {
    config: EXAM_CONFIG,
    careType: 'INITIAL' as CareType,
    showTier1: false,
    verdict: 'ACCEPT',
    rejectReason: '',
    physicalExamConfirmed: true,
    examMode: 'NORMAL',
    abnormalFindings: '',
    effectiveFindings: 'All systems within normal limits.',
    effectiveDiagnosis: 'Clinically healthy',
    trainingDecision: 'ALLOWED',
    restrictionDetails: '',
    followUpActive: false,
    followUpDate: '',
    metrics: { heartRate: '36', temperature: '37.8', weight: '500' },
    markerCount: 0,
    horseId: 1,
    vaccinationDrafts: [],
    deferVaccinationDays: null,
    ...overrides,
  };
}

describe('validateExam', () => {
  it('returns only the reject reason when the admission is rejected', () => {
    const result = validateExam(base({ showTier1: true, verdict: 'REJECT' }));

    assert.deepEqual(Object.keys(result.errors), ['rejectReason']);
    assert.equal(result.vaccinationErrors, null);
  });

  it('requires a restriction note when training is blocked', () => {
    const result = validateExam(base({ trainingDecision: 'BLOCKED', restrictionDetails: '   ' }));

    assert.ok(result.errors.restrictionDetails);
  });

  it('demands all six vitals for a standard INITIAL exam', () => {
    const result = validateExam(base({ metrics: {} }));

    assert.ok(result.errors.heartRate);
    assert.ok(result.errors.hydrationStatus);
    assert.equal(result.errors.metrics, 'All six vital signs are required before completing this examination.');
  });

  it('accepts a partially filled vitals set outside the six-vitals flows', () => {
    const result = validateExam(base({ careType: 'URGENT' as CareType, metrics: { heartRate: '60' } }));

    assert.equal(result.errors.metrics, undefined);
    assert.equal(result.errors.heartRate, undefined);
  });

  it('requires a follow-up date when follow-up care is active', () => {
    const result = validateExam(base({ followUpActive: true, followUpDate: '' }));

    assert.ok(result.errors.followUpDate);
  });

  it('rejects a follow-up date that is not at least tomorrow', () => {
    const today = validateExam(base({ followUpActive: true, followUpDate: businessDateFromToday(0) }));
    const nextWeek = validateExam(base({ followUpActive: true, followUpDate: businessDateFromToday(7) }));

    assert.ok(today.errors.followUpDate);
    assert.equal(nextWeek.errors.followUpDate, undefined);
  });

  it('requires an injury marker when a newly admitted horse stays blocked', () => {
    const withoutMarker = validateExam(
      base({ trainingDecision: 'BLOCKED', restrictionDetails: 'Box rest 3 days', markerCount: 0 }),
    );
    const withMarker = validateExam(
      base({ trainingDecision: 'BLOCKED', restrictionDetails: 'Box rest 3 days', markerCount: 1 }),
    );

    assert.ok(withoutMarker.errors.injuryMarkers);
    assert.equal(withMarker.errors.injuryMarkers, undefined);
  });

  it('requires at least one injury marker when incidentType is TRAUMA', () => {
    const withoutMarker = validateExam(
      base({
        careType: 'URGENT' as CareType,
        incidentType: 'TRAUMA',
        trainingDecision: 'BLOCKED',
        restrictionDetails: 'Tendon strain rest',
        markerCount: 0,
      }),
    );
    const withMarker = validateExam(
      base({
        careType: 'URGENT' as CareType,
        incidentType: 'TRAUMA',
        trainingDecision: 'BLOCKED',
        restrictionDetails: 'Tendon strain rest',
        markerCount: 1,
      }),
    );

    assert.equal(
      withoutMarker.errors.injuryMarkers,
      'A trauma case must record at least one injury marker before it can be completed.',
    );
    assert.equal(withMarker.errors.injuryMarkers, undefined);
  });

  it('returns per-dose vaccination errors only when vaccination applies', () => {
    const notApplicable = validateExam(
      base({ config: { ...EXAM_CONFIG, vaccination: false } }),
    );
    const incompleteDose = validateExam(
      base({
        vaccinationDrafts: [
          {
            key: 'k1',
            protocolId: 1,
            dosage: '',
            route: 'IM',
            source: 'ADMINISTERED',
            administeredOn: '',
          },
        ],
      }),
    );

    assert.equal(notApplicable.vaccinationErrors, null);
    assert.ok(incompleteDose.vaccinationErrors?.k1);
    assert.ok(incompleteDose.errors.vaccinations);
  });
});
