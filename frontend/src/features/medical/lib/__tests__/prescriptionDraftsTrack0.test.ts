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
        startDate: '2026-11-01',
        endDate: '2026-11-06',
        instructions: 'With grain feed',
      },
      {
        id: '2',
        medicationName: 'Flunixin Meglumine',
        dosage: '1.1 mg/kg',
        route: 'TOPICAL',
        frequency: 'ONCE',
        startDate: '2026-11-01',
        endDate: '2026-11-04',
        instructions: '',
      },
    ];

    const result = toPrescriptionRequests(items);
    assert.strictEqual(result.length, 2);
    assert.strictEqual(result[0].medicationName, 'Phenylbutazone');
    assert.strictEqual(result[0].dosage, '4.4 mg/kg');
    assert.strictEqual(result[0].route, 'ORAL');
    assert.strictEqual(result[0].frequency, 'BID');
    assert.strictEqual(result[0].endDate, '2026-11-06');
    assert.strictEqual(result[0].instructions, 'With grain feed');

    // Blank notes become undefined
    assert.strictEqual(result[1].instructions, '');
  });

  it('toPrescriptionRequests filters out items with empty medicationName', () => {
    const items: PrescriptionFormItem[] = [
      {
        id: '1',
        medicationName: '   ',
        dosage: '10ml',
        route: 'ORAL',
        frequency: 'SID',
        startDate: '2026-11-01',
        endDate: '2026-11-02',
        instructions: '',
      },
      {
        id: '2',
        medicationName: 'Omeprazole',
        dosage: '4 mg/kg',
        route: 'ORAL',
        frequency: 'SID',
        startDate: '2026-11-01',
        endDate: '2026-11-08',
        instructions: '',
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

  it('computeSuggestedRecheckDate returns date when prescription end dates exist', () => {
    const recheck = computeSuggestedRecheckDate(['2026-11-06', '2026-11-11'], []);
    assert.ok(recheck !== undefined);
    assert.match(recheck, /^\d{4}-\d{2}-\d{2}$/);
  });
});
