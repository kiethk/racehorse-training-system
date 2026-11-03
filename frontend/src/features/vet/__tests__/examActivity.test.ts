import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

import { examActivityByWeek } from '../lib/dashboardUtils.ts';
import type { CareSchedule } from '../../admissions/types/index.ts';

function exam(over: Partial<CareSchedule>): CareSchedule {
  return over as CareSchedule;
}

const TODAY = '2026-10-29'; // Thursday → week starts Monday 2026-10-26; 12-week window opens 2026-08-10

describe('examActivityByWeek (vet dashboard activity chart)', () => {
  it('buckets completed exams by Monday week, skips empty weeks and tracks urgent/in-progress', () => {
    const items = examActivityByWeek(
      [
        exam({ scheduledAt: '2026-10-28T09:00:00', status: 'COMPLETED', careType: 'ROUTINE' }),
        exam({ scheduledAt: '2026-10-26T09:00:00', status: 'COMPLETED', careType: 'URGENT' }),
        exam({ scheduledAt: '2026-10-27T09:00:00', status: 'IN_PROGRESS', careType: 'INITIAL' }),
        exam({ scheduledDate: '2026-10-21', status: 'COMPLETED', careType: 'ROUTINE' }),
        exam({ scheduledAt: '2026-10-15T09:00:00', status: 'CANCELLED', careType: 'ROUTINE' }),
        exam({ scheduledAt: '2026-11-02T09:00:00', status: 'SCHEDULED', careType: 'ROUTINE' }),
      ],
      TODAY,
    );

    assert.equal(items.fromDate, '2026-08-10');
    assert.equal(items.toDate, TODAY);
    assert.deepEqual(
      items.items.map((w) => [w.weekStart, w.completed, w.urgent, w.inProgress]),
      [
        ['2026-10-19', 1, 0, 0],
        ['2026-10-26', 2, 1, 1],
      ],
      'weeks ascending; cancelled and future exams draw nothing',
    );
  });

  it('excludes schedules outside the 12-week window and returns no items when only old exams exist', () => {
    const { items, fromDate } = examActivityByWeek(
      [
        exam({ scheduledAt: '2026-08-03T09:00:00', status: 'COMPLETED', careType: 'ROUTINE' }),
        exam({ scheduledAt: '2026-10-20T09:00:00', status: 'SCHEDULED', careType: 'ROUTINE' }),
      ],
      TODAY,
    );
    assert.equal(fromDate, '2026-08-10');
    assert.deepEqual(items, [], 'exam older than the window and not-yet-held exams never bucket');
  });
});
