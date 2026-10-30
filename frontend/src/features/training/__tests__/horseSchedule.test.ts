import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { groupByDate, scheduleDates, sessionState } from '../lib/horseSchedule.ts';
import type { HorseScheduleItem } from '../types';

function item(overrides: Partial<HorseScheduleItem> = {}): HorseScheduleItem {
  return {
    lotId: 1,
    lotDate: '2026-10-29',
    startTime: '06:00:00',
    endTime: '07:00:00',
    subjectName: 'Gallop',
    lotStatus: 'SCHEDULED',
    workoutStatus: 'SCHEDULED',
    ...overrides,
  };
}

describe('sessionState', () => {
  const now = new Date(2026, 9, 29, 8, 0); // 29 Oct 2026, 08:00 local

  it('is upcoming while the session has not ended', () => {
    assert.equal(sessionState(item({ lotDate: '2026-10-29', endTime: '09:30:00' }), now), 'UPCOMING');
    assert.equal(sessionState(item({ lotDate: '2026-10-30' }), now), 'UPCOMING');
  });

  it('is completed once the session time is over, even if no result was recorded', () => {
    assert.equal(sessionState(item({ lotDate: '2026-10-29', endTime: '07:00:00' }), now), 'COMPLETED');
    assert.equal(sessionState(item({ lotDate: '2026-10-20' }), now), 'COMPLETED');
  });

  it('is completed when the workout or the lot is marked completed', () => {
    assert.equal(sessionState(item({ lotDate: '2026-11-05', workoutStatus: 'COMPLETED' }), now), 'COMPLETED');
    assert.equal(sessionState(item({ lotDate: '2026-11-05', lotStatus: 'COMPLETED' }), now), 'COMPLETED');
  });

  it('is cancelled when either the lot or this horse workout is cancelled, whatever the time', () => {
    assert.equal(sessionState(item({ lotStatus: 'CANCELLED' }), now), 'CANCELLED');
    assert.equal(sessionState(item({ workoutStatus: 'CANCELLED', lotDate: '2026-10-01' }), now), 'CANCELLED');
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
      item({ lotId: 3, lotDate: '2026-10-30', startTime: '08:00:00' }),
      item({ lotId: 2, lotDate: '2026-10-29', startTime: '07:30:00' }),
      item({ lotId: 1, lotDate: '2026-10-29', startTime: '06:00:00' }),
    ]);
    assert.deepEqual(grouped.get('2026-10-29')?.map((s) => s.lotId), [1, 2]);
    assert.deepEqual(grouped.get('2026-10-30')?.map((s) => s.lotId), [3]);
    assert.equal(grouped.has('2026-10-31'), false);
  });
});
