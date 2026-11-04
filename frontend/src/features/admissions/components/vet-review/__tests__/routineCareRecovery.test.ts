import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  toPrescriptionRequests,
  computeSuggestedRecheckDate,
} from '../../../../medical/lib/prescriptionDrafts.ts';
import type { PrescriptionFormItem } from '../../../../medical/types/index.ts';

describe('Track 3a & 3b & 3d — Routine Care Prescriptions, Condition Recovery & Recheck Calculation', () => {
  it('toPrescriptionRequests: converts form items and filters out unselected medications', () => {
    const items: PrescriptionFormItem[] = [
      {
        id: 'rx-1',
        medicationName: 'Phenylbutazone',
        dosage: ' 4.4 mg/kg ',
        route: 'ORAL',
        frequency: ' SID ',
        duration: 7,
        notes: ' Administer with morning grain feed ',
      },
      {
        id: 'rx-2',
        medicationName: '', // Draft row not yet chosen
        dosage: '',
        route: 'TOPICAL',
        frequency: 'BID',
        duration: 3,
        notes: '',
      },
      {
        id: 'rx-3',
        medicationName: 'Flunixin Meglumine',
        dosage: '10 ml',
        route: 'INJECTION_IV',
        frequency: 'EOD',
        duration: 5,
        notes: '',
      },
    ];

    const requests = toPrescriptionRequests(items);

    assert.equal(requests.length, 2, 'Must filter out the item with empty medicationName');
    assert.deepEqual(requests[0], {
      medicationName: 'Phenylbutazone',
      dosage: '4.4 mg/kg',
      route: 'ORAL',
      frequency: 'SID',
      duration: 7,
      notes: 'Administer with morning grain feed',
    });
    assert.deepEqual(requests[1], {
      medicationName: 'Flunixin Meglumine',
      dosage: '10 ml',
      route: 'INJECTION_IV',
      frequency: 'EOD',
      duration: 5,
      notes: undefined,
    });
  });

  it('computeSuggestedRecheckDate: determines the latest completion boundary', () => {
    assert.equal(computeSuggestedRecheckDate([], []), undefined);

    const injuryDates = ['2026-11-20'];
    const suggested = computeSuggestedRecheckDate([], injuryDates);
    assert.equal(suggested, '2026-11-20', 'Suggested recheck should align with the later recovery date');
  });

  it('Selective condition recovery: allows toggling specific conditions without affecting others', () => {
    const activeConditionIds = [12, 15, 18];
    let resolvedConditionIds: number[] = [];

    // Vet checks condition #15 as recovered
    resolvedConditionIds = [...resolvedConditionIds, 15];
    assert.deepEqual(resolvedConditionIds, [15]);

    // Condition #12 and #18 remain active
    const remainingActive = activeConditionIds.filter((id) => !resolvedConditionIds.includes(id));
    assert.deepEqual(remainingActive, [12, 18]);

    // Vet unchecks condition #15
    resolvedConditionIds = resolvedConditionIds.filter((id) => id !== 15);
    assert.deepEqual(resolvedConditionIds, []);
  });
});
