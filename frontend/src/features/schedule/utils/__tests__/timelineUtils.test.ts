import test from 'node:test';
import assert from 'node:assert/strict';
// @ts-expect-error Node test runner with strip-types requires explicit .ts extension
import { computeTimelinePosition, getCareTypeTone } from '../timelineUtils.ts';

test('computeTimelinePosition accurately calculates 08:00 - 17:00 coordinates', () => {
  // 08:00 start (0% left), 30 minutes duration (30/540 * 100 = 5.55%)
  const pos1 = computeTimelinePosition('2026-10-18T08:00:00', 30);
  assert.equal(pos1.leftPercent, 0);
  assert.ok(Math.abs(pos1.widthPercent - 5.55) < 0.1);

  // 12:30 start (270 minutes after 08:00 = 50% left), 60 minutes duration (60/540 * 100 = 11.11%)
  const pos2 = computeTimelinePosition('2026-10-18T12:30:00', 60);
  assert.ok(Math.abs(pos2.leftPercent - 50) < 0.1);
  assert.ok(Math.abs(pos2.widthPercent - 11.11) < 0.1);

  // Clamping before 08:00 (e.g., 07:00)
  const pos3 = computeTimelinePosition('2026-10-18T07:00:00', 30);
  assert.equal(pos3.leftPercent, 0);
});

test('getCareTypeTone returns expected color schemes', () => {
  assert.equal(getCareTypeTone('URGENT').pillTone, 'danger');
  assert.equal(getCareTypeTone('INITIAL').pillTone, 'info');
  assert.equal(getCareTypeTone('ROUTINE').pillTone, 'neutral');
});
