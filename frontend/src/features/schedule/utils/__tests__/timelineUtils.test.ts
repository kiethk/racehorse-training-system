import test from 'node:test';
import assert from 'node:assert/strict';
import {
  assignTimelineLanes,
  computeTimelinePosition,
  formatClockLabel,
  getCareTypeTone,
  getMondayOf,
  getWeekDates,
  isOutsideWorkday,
  timelineRulerMarks,
  VET_WINDOW_END,
  VET_WINDOW_LEN,
  VET_WINDOW_START,
  WORKDAY_END,
  WORKDAY_LEN,
  WORKDAY_START,
} from '../timelineUtils.ts';

test('the routine window and the working day match the backend policy', () => {
  assert.equal(WORKDAY_START, 480);
  assert.equal(WORKDAY_END, 1020);
  assert.equal(WORKDAY_LEN, 540);
  assert.equal(VET_WINDOW_START, 810);
  assert.equal(VET_WINDOW_END, 930);
  assert.equal(VET_WINDOW_LEN, 120);
});

test('computeTimelinePosition uses the 08:00-17:00 day as the default range', () => {
  // 08:00 is the left edge; a 30-minute exam spans 30/540 of the day.
  const pos0 = computeTimelinePosition('2026-10-18T08:00:00', 30);
  assert.equal(pos0.leftPercent, 0);
  assert.ok(Math.abs(pos0.widthPercent - (30 / 540) * 100) < 0.1);

  // 13:30 sits (810-480)/540 = 61.11% into the day.
  const pos1 = computeTimelinePosition('2026-10-18T13:30:00', 30);
  assert.ok(Math.abs(pos1.leftPercent - 61.11) < 0.1);

  // 14:30 with a 60-minute duration.
  const pos2 = computeTimelinePosition('2026-10-18T14:30:00', 60);
  assert.ok(Math.abs(pos2.leftPercent - ((870 - 480) / 540) * 100) < 0.1);
  assert.ok(Math.abs(pos2.widthPercent - (60 / 540) * 100) < 0.1);

  // Before 08:00 clamps to the left edge; after 17:00 clamps to the right edge.
  const posEarly = computeTimelinePosition('2026-10-18T06:30:00', 30);
  assert.equal(posEarly.leftPercent, 0);
  const posLate = computeTimelinePosition('2026-10-18T22:47:00', 30);
  assert.equal(posLate.leftPercent, 100);
  assert.equal(posLate.widthPercent, 2);

  // The old window parameters still work for callers that pass them explicitly.
  const posWindowed = computeTimelinePosition('2026-10-18T14:30:00', 60, VET_WINDOW_START, VET_WINDOW_LEN);
  assert.ok(Math.abs(posWindowed.leftPercent - 50) < 0.1);
});

test('formatClockLabel renders minutes since midnight as HH:MM', () => {
  assert.equal(formatClockLabel(WORKDAY_START), '08:00');
  assert.equal(formatClockLabel(VET_WINDOW_START), '13:30');
  assert.equal(formatClockLabel(VET_WINDOW_END), '15:30');
  assert.equal(formatClockLabel(0), '00:00');
});

test('timelineRulerMarks ticks the working day hour by hour', () => {
  const marks = timelineRulerMarks();
  assert.deepEqual(marks.map((m) => m.label), [
    '08:00', '09:00', '10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00',
  ]);
  assert.equal(marks[0].percent, 0);
  assert.equal(marks[marks.length - 1].percent, 100);
});

test('isOutsideWorkday flags exams before 08:00 or from 17:00', () => {
  assert.equal(isOutsideWorkday('2026-10-18T07:30:00'), true);
  assert.equal(isOutsideWorkday('2026-10-18T08:00:00'), false);
  assert.equal(isOutsideWorkday('2026-10-18T16:59:00'), false);
  assert.equal(isOutsideWorkday('2026-10-18T17:00:00'), true);
  assert.equal(isOutsideWorkday('2026-10-18T22:47:00'), true);
});

test('getCareTypeTone labels admission, routine, preventive, follow-up and urgent exams', () => {
  assert.equal(getCareTypeTone('URGENT').label, 'Urgent');
  assert.equal(getCareTypeTone('URGENT').pillTone, 'danger');

  assert.equal(getCareTypeTone('INITIAL').label, 'Admission');
  assert.equal(getCareTypeTone('INITIAL').pillTone, 'info');

  assert.equal(getCareTypeTone('ROUTINE').label, 'Routine');
  assert.equal(getCareTypeTone('ROUTINE', null).label, 'Routine');

  assert.equal(getCareTypeTone('ROUTINE', 'PREVENTIVE').label, 'Preventive');
  assert.equal(getCareTypeTone('ROUTINE', 'PREVENTIVE').pillTone, 'primary');

  assert.equal(getCareTypeTone('ROUTINE', 'FOLLOW_UP').label, 'Follow-up');
  assert.equal(getCareTypeTone('ROUTINE', 'FOLLOW_UP').pillTone, 'neutral');

  // INITIAL keeps the Admission label even when the seed stored a purpose on it.
  assert.equal(getCareTypeTone('INITIAL', 'FOLLOW_UP').label, 'Admission');
});

test('assignTimelineLanes keeps touching or far-apart blocks on one lane', () => {
  const single = assignTimelineLanes([{ id: 1, start: 480, end: 510 }]);
  assert.equal(single.lanes, 1);
  assert.equal(single.laneOf.get(1), 0);

  const empty = assignTimelineLanes([]);
  assert.equal(empty.lanes, 1);

  // 14:00-14:30 and 14:30-15:00 touch at the boundary: still one lane.
  const touching = assignTimelineLanes([
    { id: 1, start: 840, end: 870 },
    { id: 2, start: 870, end: 900 },
  ]);
  assert.equal(touching.lanes, 1);
  assert.equal(touching.laneOf.get(2), 0);
});

test('assignTimelineLanes stacks overlapping blocks on separate lanes', () => {
  const cases = [
    { id: 1, start: 840, end: 880 }, // 14:00-14:40
    { id: 2, start: 850, end: 890 }, // 14:10-14:50 overlaps 1
    { id: 3, start: 890, end: 920 }, // 14:50-15:20 overlaps 2, free vs 1
    { id: 4, start: 480, end: 510 }, // 08:00 far earlier, packed first
  ];
  const { lanes, laneOf } = assignTimelineLanes(cases);
  assert.equal(lanes, 2);
  assert.equal(laneOf.get(4), 0);
  assert.equal(laneOf.get(1), 0);
  assert.equal(laneOf.get(2), 1);
  // Lane 0 ends at 880, so block 3 (890) can reuse it.
  assert.equal(laneOf.get(3), 0);
});

test('getMondayOf and getWeekDates compute accurate 7-day week intervals', () => {
  // 2026-11-02 is Monday
  assert.equal(getMondayOf('2026-11-02'), '2026-11-02');
  // 2026-11-08 is Sunday of the same week
  assert.equal(getMondayOf('2026-11-08'), '2026-11-02');
  // 2026-11-05 is Thursday
  assert.equal(getMondayOf('2026-11-05'), '2026-11-02');

  const days = getWeekDates('2026-11-02');
  assert.equal(days.length, 7);
  assert.equal(days[0], '2026-11-02');
  assert.equal(days[6], '2026-11-08');
});
