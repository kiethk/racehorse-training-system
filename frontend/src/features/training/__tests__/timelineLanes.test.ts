import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { assignLanes } from '../lib/timeline.ts';

const session = (id: number, startTime: string, endTime: string) => ({ id, startTime, endTime });

describe('assignLanes', () => {
  it('keeps sessions that do not overlap in one row', () => {
    const { laneOf, laneCount } = assignLanes([session(1, '06:00', '06:45'), session(2, '06:45', '07:30')]);
    assert.equal(laneCount, 1);
    assert.equal(laneOf.get(1), 0);
    assert.equal(laneOf.get(2), 0);
  });

  it('gives sessions at the same time their own rows, so none is hidden behind another', () => {
    const { laneOf, laneCount } = assignLanes([session(1, '06:00', '06:45'), session(2, '06:00', '06:45'), session(3, '06:00', '07:00')]);
    assert.equal(laneCount, 3);
    assert.deepEqual([laneOf.get(1), laneOf.get(2), laneOf.get(3)].sort(), [0, 1, 2]);
  });

  it('reuses a free row once an earlier session has ended', () => {
    // 1 and 2 overlap; 3 starts when 1 ends, so it goes back to the first row.
    const { laneOf, laneCount } = assignLanes([session(1, '06:00', '07:00'), session(2, '06:45', '07:30'), session(3, '07:00', '08:00')]);
    assert.equal(laneCount, 2);
    assert.equal(laneOf.get(3), laneOf.get(1));
    assert.notEqual(laneOf.get(2), laneOf.get(1));
  });

  it('needs one row even when there is nothing to draw', () => {
    assert.equal(assignLanes([]).laneCount, 1);
  });
});
