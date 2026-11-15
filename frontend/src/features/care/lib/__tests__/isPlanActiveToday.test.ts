import test from 'node:test';
import assert from 'node:assert/strict';
import { isPlanActiveToday } from '../isPlanActiveToday.ts';
import type { TreatmentPlan } from '../../types/index.ts';

const plan: TreatmentPlan = {
  id: 1,
  healthRecordId: 2,
  treatmentName: 'Treatment',
  instructions: 'Follow instructions',
  startDate: '2026-11-01',
  endDate: '2026-11-05',
  status: 'ACTIVE',
};

test('active treatment is active through its end date', () => {
  assert.equal(isPlanActiveToday(plan, '2026-11-05'), true);
  assert.equal(isPlanActiveToday(plan, '2026-11-06'), false);
});

test('inactive status is excluded even when the end date is in the future', () => {
  assert.equal(isPlanActiveToday({ ...plan, status: 'COMPLETED' }, '2026-11-04'), false);
});
