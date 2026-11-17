import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { scheduleDates } from '../lib/horseSchedule.ts';

describe('scheduleDates', () => {
  it('lists seven days starting at the chosen day, across a month end', () => {
    assert.deepEqual(scheduleDates('2026-10-29'), [
      '2026-10-29', '2026-10-30', '2026-10-31', '2026-11-01', '2026-11-02', '2026-11-03', '2026-11-04',
    ]);
  });
});
