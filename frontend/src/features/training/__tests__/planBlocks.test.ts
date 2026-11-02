import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { groupPlansByGroup } from '../lib/planBlocks.ts';
import type { GroupPlanSummary, TrainingGroup } from '../types';

function group(overrides: Partial<TrainingGroup>): TrainingGroup {
  return {
    id: 1, name: 'Alpha', trainerId: 9, members: [], currentCourseName: null,
    currentPlanStatus: null, currentPlanEndDate: null, disbanded: false, ...overrides,
  };
}

function plan(overrides: Partial<GroupPlanSummary>): GroupPlanSummary {
  return {
    id: 1, groupId: 1, groupName: 'Alpha', groupDisbanded: false, courseId: 1, courseName: 'Sprint',
    startDate: '2026-11-02', endDate: '2026-11-20', status: 'ACTIVE', sessionsHeld: 0, sessionsCancelled: 0, sessionsTotal: 5,
    ...overrides,
  };
}

const members = (n: number) => Array.from({ length: n }, (_, i) => ({ horseId: i + 1, horseName: `H${i}` })) as unknown as TrainingGroup['members'];

describe('groupPlansByGroup', () => {
  it('puts live groups first and Disbanded groups last', () => {
    const blocks = groupPlansByGroup(
      [group({ id: 1, name: 'Alpha' }), group({ id: 2, name: 'Beta' })],
      [plan({ id: 5, groupId: 9, groupName: 'Old', groupDisbanded: true, status: 'CANCELLED' }), plan({ id: 6, groupId: 2, groupName: 'Beta' })],
    );
    assert.deepEqual(blocks.map((b) => [b.name, b.disbanded]), [['Alpha', false], ['Beta', false], ['Old', true]]);
  });

  it('keeps a live group that has no plan, with its horse count', () => {
    const [block] = groupPlansByGroup([group({ members: members(3) })], []);
    assert.equal(block.horseCount, 3);
    assert.deepEqual(block.plans, []);
  });

  it('lists unfinished plans first, then finished ones, newest first in each part', () => {
    const [block] = groupPlansByGroup([group({})], [
      plan({ id: 1, status: 'COMPLETED' }),
      plan({ id: 4, status: 'CANCELLED' }),
      plan({ id: 2, status: 'UPCOMING' }),
      plan({ id: 3, status: 'ACTIVE' }),
    ]);
    assert.deepEqual(block.plans.map((p) => p.id), [3, 2, 4, 1]);
  });

  it('counts plans and gives a Disbanded group no horses', () => {
    const blocks = groupPlansByGroup([group({ members: members(2) })], [
      plan({ id: 1 }), plan({ id: 2 }),
      plan({ id: 3, groupId: 7, groupName: 'Old', groupDisbanded: true, status: 'CANCELLED' }),
    ]);
    assert.equal(blocks[0].plans.length, 2);
    assert.equal(blocks[1].horseCount, 0);
    assert.equal(blocks[1].groupId, 7);
  });
});
