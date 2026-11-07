import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { getScheduleExamAction, getScheduleStatusDisplay } from '../scheduleStatusDisplay.ts';

describe('scheduleStatusDisplay', () => {
  it('correctly maps canonical statuses', () => {
    const scheduled = getScheduleStatusDisplay('SCHEDULED');
    const inProgress = getScheduleStatusDisplay('IN_PROGRESS');
    const completed = getScheduleStatusDisplay('COMPLETED');
    const cancelled = getScheduleStatusDisplay('CANCELLED');
    assert.deepEqual([scheduled.label, scheduled.tone], ['Scheduled', 'neutral']);
    assert.deepEqual([inProgress.label, inProgress.tone], ['In Progress', 'primary']);
    assert.deepEqual([completed.label, completed.tone], ['Completed', 'success']);
    assert.deepEqual([cancelled.label, cancelled.tone], ['Cancelled', 'danger']);
  });

  it('labels every canonical status and falls back for unknown ones', () => {
    for (const status of ['SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']) {
      assert.notEqual(getScheduleStatusDisplay(status).label, 'Unknown');
    }
    assert.equal(getScheduleStatusDisplay(undefined).label, 'Unknown');
  });

  it('unifies open exam actions as Examine and completed ones as View Record', () => {
    assert.equal(getScheduleExamAction('IN_PROGRESS').label, 'Examine');
    assert.equal(getScheduleExamAction('SCHEDULED').label, 'Examine');
    assert.equal(getScheduleExamAction('COMPLETED').label, 'View Record');
  });
});
