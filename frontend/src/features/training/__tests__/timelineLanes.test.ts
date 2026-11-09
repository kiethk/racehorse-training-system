import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { assignLanes, assignTrainerLanes } from '../lib/timeline.ts';

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

describe('assignTrainerLanes', () => {
  const own = (id: number, trainerId: number, trainerName: string, startTime: string, endTime: string) => ({
    id,
    trainerId,
    trainerName,
    startTime,
    endTime,
  });

  it('gives each Trainer a row of their own, in name order, so two Trainers at the same time never share one', () => {
    const { laneOf, rows, laneCount } = assignTrainerLanes([
      own(1, 20, 'Sarah Bennett', '06:00', '06:45'),
      own(2, 10, 'Daniel Foster', '06:00', '07:00'),
      own(3, 10, 'Daniel Foster', '07:00', '08:00'),
      own(4, 30, 'Michael Turner', '06:00', '06:45'),
    ]);
    assert.equal(laneCount, 3);
    assert.deepEqual(rows.map((row) => [row.trainerName, row.firstLane, row.laneCount]), [
      ['Daniel Foster', 0, 1],
      ['Michael Turner', 1, 1],
      ['Sarah Bennett', 2, 1],
    ]);
    // Daniel Foster's two back-to-back sessions share one row.
    assert.equal(laneOf.get(2), 0);
    assert.equal(laneOf.get(3), 0);
    assert.equal(laneOf.get(4), 1);
    assert.equal(laneOf.get(1), 2);
  });

  it('keeps a single Trainer in one row when their sessions do not overlap', () => {
    const { rows, laneCount } = assignTrainerLanes([own(1, 10, 'Daniel Foster', '06:00', '07:00'), own(2, 10, 'Daniel Foster', '07:00', '08:00')]);
    assert.equal(laneCount, 1);
    assert.equal(rows.length, 1);
  });
});
