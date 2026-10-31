import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { groupByDate, scheduleDates, sessionState } from '../lib/horseSchedule.ts';
import type { HorseScheduleItem } from '../types';

function item(overrides: Partial<HorseScheduleItem> = {}): HorseScheduleItem {
  return {
    sessionId: 1,
    sessionDate: '2026-10-29',
    startTime: '06:00:00',
    endTime: '07:00:00',
    subjectName: 'Gallop',
    sessionStatus: 'SCHEDULED',
    workoutStatus: 'SCHEDULED',
    ...overrides,
  };
}

describe('sessionState', () => {
  const now = new Date(2026, 9, 29, 8, 0); // 29 Oct 2026, 08:00 local

  it('is upcoming while the session has not ended', () => {
    assert.equal(sessionState(item({ sessionDate: '2026-10-29', endTime: '09:30:00' }), now), 'UPCOMING');
    assert.equal(sessionState(item({ sessionDate: '2026-10-30' }), now), 'UPCOMING');
  });

  it('is completed once the session time is over, even if no result was recorded', () => {
    assert.equal(sessionState(item({ sessionDate: '2026-10-29', endTime: '07:00:00' }), now), 'HELD');
    assert.equal(sessionState(item({ sessionDate: '2026-10-20' }), now), 'HELD');
  });

  it('is held when the workout or the session is held', () => {
    assert.equal(sessionState(item({ sessionDate: '2026-11-05', workoutStatus: 'HELD' }), now), 'HELD');
    assert.equal(sessionState(item({ sessionDate: '2026-11-05', sessionStatus: 'HELD' }), now), 'HELD');
  });

  it('is cancelled when either the session or this horse workout is cancelled, whatever the time', () => {
    assert.equal(sessionState(item({ sessionStatus: 'CANCELLED' }), now), 'CANCELLED');
    assert.equal(sessionState(item({ workoutStatus: 'CANCELLED', sessionDate: '2026-10-01' }), now), 'CANCELLED');
  });
});

describe('scheduleDates', () => {
  it('lists seven days starting at the chosen day, across a month end', () => {
    assert.deepEqual(scheduleDates('2026-10-29'), [
      '2026-10-29', '2026-10-30', '2026-10-31', '2026-11-01', '2026-11-02', '2026-11-03', '2026-11-04',
    ]);
  });
});

describe('groupByDate', () => {
  it('groups sessions by day and sorts each day by start time', () => {
    const grouped = groupByDate([
      item({ sessionId: 3, sessionDate: '2026-10-30', startTime: '08:00:00' }),
      item({ sessionId: 2, sessionDate: '2026-10-29', startTime: '07:30:00' }),
      item({ sessionId: 1, sessionDate: '2026-10-29', startTime: '06:00:00' }),
    ]);
    assert.deepEqual(grouped.get('2026-10-29')?.map((s) => s.sessionId), [1, 2]);
    assert.deepEqual(grouped.get('2026-10-30')?.map((s) => s.sessionId), [3]);
    assert.equal(grouped.has('2026-10-31'), false);
  });
});
