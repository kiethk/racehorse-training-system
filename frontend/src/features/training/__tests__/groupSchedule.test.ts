import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { attendanceLabel, groupByDate, mondayOf, sessionHasStarted, totalNeedingResult, weekDates } from '../lib/groupSchedule.ts';
import type { SessionSummary } from '../types';

function session(overrides: Partial<SessionSummary>): SessionSummary {
  return {
    id: 1, courseRunId: 1, groupId: 1, groupName: 'Nhom 1', groupDisbanded: false, courseName: 'Sprint', exerciseName: 'S1',
    date: '2026-11-04', startTime: '06:00:00', endTime: '06:30:00', status: 'SCHEDULED',
    horseCount: 3, needsResultCount: 0, trainerId: 1, trainerName: 'Trainer 1', ...overrides,
  };
}

describe('mondayOf', () => {
  it('keeps a Monday', () => assert.equal(mondayOf('2026-11-02'), '2026-11-02'));
  it('moves a Wednesday back to its Monday', () => assert.equal(mondayOf('2026-11-04'), '2026-11-02'));
  it('moves a Sunday back to the Monday before it', () => assert.equal(mondayOf('2026-11-08'), '2026-11-02'));
});

describe('weekDates', () => {
  it('lists seven consecutive days', () => {
    assert.deepEqual(weekDates('2026-11-02'), [
      '2026-11-02', '2026-11-03', '2026-11-04', '2026-11-05', '2026-11-06', '2026-11-07', '2026-11-08',
    ]);
  });
  it('rolls over a month end', () => assert.equal(weekDates('2026-10-30')[3], '2026-11-02'));
});

describe('groupByDate', () => {
  it('groups by date and orders each day by start time', () => {
    const days = groupByDate([
      session({ id: 1, date: '2026-11-04', startTime: '07:00:00' }),
      session({ id: 2, date: '2026-11-04', startTime: '06:00:00' }),
      session({ id: 3, date: '2026-11-06' }),
    ]);
    assert.deepEqual(days['2026-11-04'].map((s) => s.id), [2, 1]);
    assert.equal(days['2026-11-06'].length, 1);
    assert.equal(days['2026-11-05'], undefined);
  });
});

describe('sessionHasStarted', () => {
  const now = new Date(2026, 10, 4, 6, 15);   // 4 Nov 2026, 06:15
  it('is false for a later day', () => assert.equal(sessionHasStarted(session({ date: '2026-11-05' }), now), false));
  it('is true for an earlier day', () => assert.equal(sessionHasStarted(session({ date: '2026-11-03' }), now), true));
  it('is true once today start time has passed', () => assert.equal(sessionHasStarted(session({ startTime: '06:00:00' }), now), true));
  it('is false before today start time', () => assert.equal(sessionHasStarted(session({ startTime: '06:30:00' }), now), false));
});

describe('totalNeedingResult', () => {
  it('adds up the missing results of all Sessions', () => {
    assert.equal(totalNeedingResult([session({ needsResultCount: 2 }), session({ needsResultCount: 0 }), session({ needsResultCount: 1 })]), 3);
  });
});

describe('attendanceLabel', () => {
  it('reads as X of Y ran', () => assert.equal(attendanceLabel(7, 20), '7 of 20 ran'));
  it('counts against every session, so a horse that missed none of one still reads 0 of 1 ran', () => assert.equal(attendanceLabel(0, 1), '0 of 1 ran'));
});
