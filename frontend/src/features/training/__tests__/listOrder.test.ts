import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { newestFirst, courseRunOrder } from '../lib/listOrder.ts';
import type { CourseRunStatus } from '../types';

function courseRun(courseRunId: number, status: CourseRunStatus) {
  return { courseRunId, status };
}

describe('newestFirst', () => {
  it('puts the record created last first', () => {
    assert.deepEqual([{ id: 2 }, { id: 9 }, { id: 5 }].sort(newestFirst).map((row) => row.id), [9, 5, 2]);
  });
});

describe('courseRunOrder', () => {
  it('lists active course runs first, then upcoming, then completed, and cancelled last', () => {
    const sorted = [courseRun(1, 'CANCELLED'), courseRun(2, 'COMPLETED'), courseRun(3, 'UPCOMING'), courseRun(4, 'ACTIVE')].sort(courseRunOrder);
    assert.deepEqual(sorted.map((row) => row.status), ['ACTIVE', 'UPCOMING', 'COMPLETED', 'CANCELLED']);
  });

  it('puts the newest course run first within the same status', () => {
    const sorted = [courseRun(3, 'ACTIVE'), courseRun(8, 'ACTIVE'), courseRun(5, 'CANCELLED'), courseRun(9, 'CANCELLED')].sort(courseRunOrder);
    assert.deepEqual(sorted.map((row) => row.courseRunId), [8, 3, 9, 5]);
  });
});
