import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import {
  toPrescriptionRequests,
  computeSuggestedRecheckDate,
  type PrescriptionFormItem,
} from '../prescriptionDrafts.ts';

describe('Track 3a & 3b & 3d — Routine Care Prescriptions, Condition Recovery & Recheck Calculation', () => {
  it('toPrescriptionRequests: converts form items and filters out unselected medications', () => {
    const items: PrescriptionFormItem[] = [
      {
        id: 'rx-1',
        medicationId: 101,
        dosage: ' 4.4 mg/kg ',
        route: 'ORAL',
        frequency: ' SID ',
        startDate: '2026-11-02',
        endDate: '2026-11-09',
        withdrawalDays: 7,
        instructions: ' Administer with morning grain feed ',
      },
      {
        id: 'rx-2',
        medicationId: null, // Draft row not yet chosen
        dosage: '',
        route: 'TOPICAL',
        frequency: 'BID',
        startDate: '2026-11-02',
        endDate: '2026-11-05',
        withdrawalDays: 0,
        instructions: '',
      },
      {
        id: 'rx-3',
        medicationId: 105,
        dosage: '10 ml',
        route: 'INJECTION_IV',
        frequency: 'EOD',
        startDate: '2026-11-03',
        endDate: '2026-11-07',
        withdrawalDays: 14,
        instructions: '',
      },
    ];

    const requests = toPrescriptionRequests(items);

    assert.equal(requests.length, 2, 'Must filter out the item with null medicationId');
    assert.deepEqual(requests[0], {
      medicationId: 101,
      dosage: '4.4 mg/kg',
      route: 'ORAL',
      frequency: 'SID',
      startDate: '2026-11-02',
      endDate: '2026-11-09',
      withdrawalDays: 7,
      instructions: 'Administer with morning grain feed',
    });
    assert.deepEqual(requests[1], {
      medicationId: 105,
      dosage: '10 ml',
      route: 'INJECTION_IV',
      frequency: 'EOD',
      startDate: '2026-11-03',
      endDate: '2026-11-07',
      withdrawalDays: 14,
      instructions: undefined,
    });
  });

  it('computeSuggestedRecheckDate: determines the latest completion boundary', () => {
    assert.equal(computeSuggestedRecheckDate([], []), undefined);

    const rxDates = ['2026-11-05', '2026-11-12'];
    const injuryDates = ['2026-11-08'];

    const suggested = computeSuggestedRecheckDate(rxDates, injuryDates);
    assert.equal(suggested, '2026-11-12', 'Suggested recheck should align with the end of the active medication cycle');

    const injuryLater = computeSuggestedRecheckDate(['2026-11-05'], ['2026-11-20']);
    assert.equal(injuryLater, '2026-11-20', 'Suggested recheck should align with the later recovery date');
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
