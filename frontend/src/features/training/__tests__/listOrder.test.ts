import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { newestFirst, planOrder } from '../lib/listOrder.ts';
import type { PlanStatus } from '../types';

function plan(planId: number, status: PlanStatus) {
  return { planId, status };
}

describe('newestFirst', () => {
  it('puts the record created last first', () => {
    assert.deepEqual([{ id: 2 }, { id: 9 }, { id: 5 }].sort(newestFirst).map((row) => row.id), [9, 5, 2]);
  });
});

describe('planOrder', () => {
  it('lists active plans first, then upcoming, then completed, and cancelled last', () => {
    const sorted = [plan(1, 'CANCELLED'), plan(2, 'COMPLETED'), plan(3, 'UPCOMING'), plan(4, 'ACTIVE')].sort(planOrder);
    assert.deepEqual(sorted.map((row) => row.status), ['ACTIVE', 'UPCOMING', 'COMPLETED', 'CANCELLED']);
  });

  it('puts the newest plan first within the same status', () => {
    const sorted = [plan(3, 'ACTIVE'), plan(8, 'ACTIVE'), plan(5, 'CANCELLED'), plan(9, 'CANCELLED')].sort(planOrder);
    assert.deepEqual(sorted.map((row) => row.planId), [8, 3, 9, 5]);
  });
});
