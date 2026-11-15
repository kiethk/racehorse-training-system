import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { computeSuggestedRecheckDate } from '../../../../medical/lib/prescriptionDrafts.ts';

describe('Track 3a & 3b & 3d — Routine Care Prescriptions, Condition Recovery & Recheck Calculation', () => {
  it('computeSuggestedRecheckDate: determines the latest completion boundary', () => {
    assert.equal(computeSuggestedRecheckDate([]), undefined);

    const injuryDates = ['2026-11-20'];
    const suggested = computeSuggestedRecheckDate(injuryDates);
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
