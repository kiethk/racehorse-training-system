import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { getScheduleExamAction, getScheduleStatusDisplay } from '../scheduleStatusDisplay.ts';

describe('scheduleStatusDisplay', () => {
  it('keeps OVERDUE (red) and DELAYED (amber) distinct', () => {
    const overdue = getScheduleStatusDisplay('OVERDUE');
    const delayed = getScheduleStatusDisplay('DELAYED');
    assert.deepEqual([overdue.label, overdue.tone], ['Overdue', 'danger']);
    assert.deepEqual([delayed.label, delayed.tone], ['Delayed', 'warning']);
  });

  it('labels every status and falls back for unknown ones', () => {
    for (const status of ['REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'PAUSED', 'DELAYED', 'OVERDUE', 'COMPLETED']) {
      assert.notEqual(getScheduleStatusDisplay(status).label, 'Unknown');
    }
    assert.equal(getScheduleStatusDisplay(undefined).label, 'Unknown');
  });

  it('uses the same action for started exams and Start Exam for overdue ones', () => {
    assert.equal(getScheduleExamAction('IN_PROGRESS').label, 'Continue Exam');
    assert.equal(getScheduleExamAction('DELAYED').label, 'Continue Exam');
    assert.equal(getScheduleExamAction('OVERDUE').label, 'Start Exam');
    assert.equal(getScheduleExamAction('SCHEDULED').label, 'Start Exam');
    assert.equal(getScheduleExamAction('COMPLETED').label, 'View Record');
  });
});
