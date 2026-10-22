import test from 'node:test';
import assert from 'node:assert/strict';
import {
  computeTimelinePosition,
  formatClockLabel,
  getCareTypeTone,
  timelineRulerMarks,
  VET_WINDOW_END,
  VET_WINDOW_LEN,
  VET_WINDOW_START,
} from '../timelineUtils.ts';

test('the vet window matches the backend policy (13:30 - 15:30)', () => {
  assert.equal(VET_WINDOW_START, 810);
  assert.equal(VET_WINDOW_END, 930);
  assert.equal(VET_WINDOW_LEN, 120);
});

test('computeTimelinePosition accurately calculates 13:30 - 15:30 coordinates', () => {
  // 13:30 start (0% left), 30 minutes duration (30/120 * 100 = 25%)
  const pos1 = computeTimelinePosition('2026-10-18T13:30:00', 30);
  assert.equal(pos1.leftPercent, 0);
  assert.ok(Math.abs(pos1.widthPercent - 25) < 0.1);

  // 14:30 start (60 minutes after 13:30 = 50% left), 60 minutes duration (60/120 * 100 = 50%)
  const pos2 = computeTimelinePosition('2026-10-18T14:30:00', 60);
  assert.ok(Math.abs(pos2.leftPercent - 50) < 0.1);
  assert.ok(Math.abs(pos2.widthPercent - 50) < 0.1);

  // 15:30 start (120 minutes after 13:30 = 100% left)
  const pos3 = computeTimelinePosition('2026-10-18T15:30:00', 30);
  assert.equal(pos3.leftPercent, 100);

  // Clamping before 13:30 (e.g., 13:00)
  const pos4 = computeTimelinePosition('2026-10-18T13:00:00', 30);
  assert.equal(pos4.leftPercent, 0);
});

test('formatClockLabel renders minutes since midnight as HH:MM', () => {
  assert.equal(formatClockLabel(VET_WINDOW_START), '13:30');
  assert.equal(formatClockLabel(VET_WINDOW_END), '15:30');
  assert.equal(formatClockLabel(0), '00:00');
});

test('timelineRulerMarks derives its ticks from the window constants', () => {
  const marks = timelineRulerMarks();
  assert.deepEqual(marks.map((m) => m.label), ['13:30', '14:00', '14:30', '15:00', '15:30']);
  assert.equal(marks[0].percent, 0);
  assert.equal(marks[marks.length - 1].percent, 100);
});

test('getCareTypeTone returns expected color schemes', () => {
  assert.equal(getCareTypeTone('URGENT').pillTone, 'danger');
  assert.equal(getCareTypeTone('INITIAL').pillTone, 'info');
  assert.equal(getCareTypeTone('ROUTINE').pillTone, 'neutral');
});
