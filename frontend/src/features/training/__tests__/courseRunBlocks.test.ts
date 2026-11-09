import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { courseRunsByGroup } from '../lib/courseRunBlocks.ts';
import type { CourseRunSummary, TrainingGroup } from '../types';

function group(overrides: Partial<TrainingGroup>): TrainingGroup {
  return {
    id: 1, name: 'Alpha', trainerId: 9, members: [], currentCourseName: null,
    currentCourseRunStatus: null, currentCourseRunEndDate: null, disbanded: false, ...overrides,
  };
}

function courseRun(overrides: Partial<CourseRunSummary>): CourseRunSummary {
  return {
    id: 1, groupId: 1, groupName: 'Alpha', groupDisbanded: false, courseId: 1, courseName: 'Sprint',
    startDate: '2026-11-02', endDate: '2026-11-20', status: 'ACTIVE', sessionsHeld: 0, sessionsCancelled: 0, sessionsTotal: 5,
    ...overrides,
  };
}

const members = (n: number) => Array.from({ length: n }, (_, i) => ({ horseId: i + 1, horseName: `H${i}` })) as unknown as TrainingGroup['members'];

describe('courseRunsByGroup', () => {
  it('puts live groups first and Disbanded groups last', () => {
    const blocks = courseRunsByGroup(
      [group({ id: 1, name: 'Alpha' }), group({ id: 2, name: 'Beta' })],
      [courseRun({ id: 5, groupId: 9, groupName: 'Old', groupDisbanded: true, status: 'CANCELLED' }), courseRun({ id: 6, groupId: 2, groupName: 'Beta' })],
    );
    assert.deepEqual(blocks.map((b) => [b.name, b.disbanded]), [['Alpha', false], ['Beta', false], ['Old', true]]);
  });

  it('keeps a live group that has no course run, with its horse count', () => {
    const [block] = courseRunsByGroup([group({ members: members(3) })], []);
    assert.equal(block.horseCount, 3);
    assert.deepEqual(block.courseRuns, []);
  });

  it('lists unfinished course runs first, then finished ones, newest first in each part', () => {
    const [block] = courseRunsByGroup([group({})], [
      courseRun({ id: 1, status: 'COMPLETED' }),
      courseRun({ id: 4, status: 'CANCELLED' }),
      courseRun({ id: 2, status: 'UPCOMING' }),
      courseRun({ id: 3, status: 'ACTIVE' }),
    ]);
    assert.deepEqual(block.courseRuns.map((p) => p.id), [3, 2, 4, 1]);
  });

  it('counts course runs and gives a Disbanded group no horses', () => {
    const blocks = courseRunsByGroup([group({ members: members(2) })], [
      courseRun({ id: 1 }), courseRun({ id: 2 }),
      courseRun({ id: 3, groupId: 7, groupName: 'Old', groupDisbanded: true, status: 'CANCELLED' }),
    ]);
    assert.equal(blocks[0].courseRuns.length, 2);
    assert.equal(blocks[1].horseCount, 0);
    assert.equal(blocks[1].groupId, 7);
  });
});
