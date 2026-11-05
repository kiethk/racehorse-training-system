import test from 'node:test';
import assert from 'node:assert/strict';
import {
  formatDate,
  formatDateTime,
  formatTime,
  formatClockLabel,
  formatEnumLabel,
  formatNumber,
  formatLocalDateIso,
} from '../display.ts';

test('formatDate renders en-GB short date or fallback', () => {
  assert.equal(formatDate(null), '—');
  assert.equal(formatDate(undefined), '—');
  assert.equal(formatDate('invalid-date'), '—');
  // Fixed timestamp check
  const formatted = formatDate('2026-11-05T08:00:00Z');
  assert.match(formatted, /\d{2}\s\w{3}\s2026/);
});

test('formatDateTime renders en-GB date and time', () => {
  assert.equal(formatDateTime(null), '—');
  const formatted = formatDateTime('2026-11-05T08:30:00Z');
  assert.match(formatted, /\d{2}\s\w{3}\s2026/);
});

test('formatTime slices time string to HH:mm', () => {
  assert.equal(formatTime('08:30:00'), '08:30');
  assert.equal(formatTime(null), '—');
});

test('formatClockLabel formats numbers, ISO timestamps, and time strings', () => {
  assert.equal(formatClockLabel(null), '—');
  assert.equal(formatClockLabel(undefined), '—');
  // Minutes since midnight
  assert.equal(formatClockLabel(480), '08:00');
  assert.equal(formatClockLabel(810), '13:30');
  // String format
  assert.equal(formatClockLabel('14:30:00'), '14:30');
  assert.equal(formatClockLabel('09:15'), '09:15');
});

test('formatEnumLabel converts SCREAMING_SNAKE to Title case', () => {
  assert.equal(formatEnumLabel('HEAD_TRAINER'), 'Head trainer');
  assert.equal(formatEnumLabel('ACTIVE'), 'Active');
  assert.equal(formatEnumLabel(null), '—');
});

test('formatNumber formats numbers with standard thousand separators', () => {
  assert.equal(formatNumber(1234567), '1,234,567');
  assert.equal(formatNumber(0), '0');
  assert.equal(formatNumber(null), '—');
});

test('formatLocalDateIso formats date to YYYY-MM-DD in VN timezone', () => {
  const result = formatLocalDateIso(new Date(2026, 10, 5, 12, 0, 0));
  assert.equal(result, '2026-11-05');
});
