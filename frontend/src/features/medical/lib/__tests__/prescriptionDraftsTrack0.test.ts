import { describe, it } from 'node:test';
import assert from 'node:assert';
import {
  toPrescriptionRequests,
  computeSuggestedRecheckDate,
} from '../prescriptionDrafts.ts';
import type { PrescriptionFormItem } from '../../types';

describe('Track 0: Medical Prescription Drafts (ROADMAP v3.0)', () => {
  it('toPrescriptionRequests converts form items to API payload with snake_case and duration', () => {
    const items: PrescriptionFormItem[] = [
      {
        id: '1',
        medicationName: 'Phenylbutazone',
        dosage: '4.4 mg/kg',
        route: 'ORAL',
        frequency: 'BID',
        duration: 5,
        notes: 'With grain feed',
      },
      {
        id: '2',
        medicationName: 'Flunixin Meglumine',
        dosage: '1.1 mg/kg',
        route: 'INJECTION_IV',
        frequency: 'ONCE',
        duration: 3,
        notes: '',
      },
    ];

    const result = toPrescriptionRequests(items);
    assert.strictEqual(result.length, 2);
    assert.strictEqual(result[0].medicationName, 'Phenylbutazone');
    assert.strictEqual(result[0].dosage, '4.4 mg/kg');
    assert.strictEqual(result[0].route, 'ORAL');
    assert.strictEqual(result[0].frequency, 'BID');
    assert.strictEqual(result[0].duration, 5);
    assert.strictEqual(result[0].notes, 'With grain feed');

    // Blank notes become undefined
    assert.strictEqual(result[1].notes, undefined);
  });

  it('toPrescriptionRequests filters out items with empty medicationName', () => {
    const items: PrescriptionFormItem[] = [
      {
        id: '1',
        medicationName: '   ',
        dosage: '10ml',
        route: 'ORAL',
        frequency: 'SID',
        duration: 1,
        notes: '',
      },
      {
        id: '2',
        medicationName: 'Omeprazole',
        dosage: '4 mg/kg',
        route: 'ORAL',
        frequency: 'SID',
        duration: 7,
        notes: '',
      },
    ];

    const result = toPrescriptionRequests(items);
    assert.strictEqual(result.length, 1);
    assert.strictEqual(result[0].medicationName, 'Omeprazole');
  });

  it('computeSuggestedRecheckDate returns undefined when no durations or dates given', () => {
    const recheck = computeSuggestedRecheckDate([], []);
    assert.strictEqual(recheck, undefined);
  });

  it('computeSuggestedRecheckDate returns date when valid prescription durations exist', () => {
    const recheck = computeSuggestedRecheckDate([5, 10], []);
    assert.ok(recheck !== undefined);
    assert.match(recheck, /^\d{4}-\d{2}-\d{2}$/);
  });
});
