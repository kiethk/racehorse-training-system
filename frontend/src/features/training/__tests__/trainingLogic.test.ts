import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { cancelledLabel, courseRunProgress } from '../lib/courseRunProgress.ts';
import { ratingDistribution, ratingTier, ratingTone } from '../lib/rating.ts';
import { COURSE_RUN_STATUS } from '../lib/status.ts';
import { clockOf, minutesOf, sessionStartOptions } from '../lib/timeline.ts';
import { rangeForPreset, rangeLabel, shiftIsoDate, toIsoDate, tomorrowIso } from '../lib/dateRange.ts';

describe('courseRunProgress', () => {
  it('keeps cancelled sessions in the total, so a course run closed with 2 of 10 cancelled reads 8/10', () => {
    const progress = courseRunProgress(8, 10, 2);
    assert.equal(progress.total, 10);
    assert.equal(progress.percent, 80);
  });

  it('reaches 100% only when every session of the course run was held', () => {
    assert.equal(courseRunProgress(10, 10, 0).percent, 100);
  });

  it('never divides by zero for an empty course run', () => {
    assert.deepEqual(courseRunProgress(0, 0, 0), { completed: 0, total: 0, cancelled: 0, percent: 0, allCancelled: false });
  });

  it('flags a course run whose sessions are all cancelled', () => {
    assert.equal(courseRunProgress(0, 6, 6).allCancelled, true);
    assert.equal(courseRunProgress(1, 6, 5).allCancelled, false);
  });

  it('labels the cancelled count in the singular and plural', () => {
    assert.equal(cancelledLabel(1), '1 session cancelled');
    assert.equal(cancelledLabel(3), '3 sessions cancelled');
  });

  it('reads a cancelled course run with the same formula: 3 held of 6 is 3/6', () => {
    const progress = courseRunProgress(3, 6, 3);
    assert.equal(progress.total, 6);
    assert.equal(progress.percent, 50);
  });
});

describe('rating tiers', () => {
  it('puts the boundaries into the higher tier', () => {
    assert.equal(ratingTier(8), 'excellent');
    assert.equal(ratingTier(7.9), 'good');
    assert.equal(ratingTier(6), 'good');
    assert.equal(ratingTier(5.9), 'needsImprovement');
  });

  it('maps each tier to one tone', () => {
    assert.equal(ratingTone(9), 'success');
    assert.equal(ratingTone(7), 'info');
    assert.equal(ratingTone(3), 'warning');
  });

  it('counts a missing rating as no data', () => {
    assert.deepEqual(ratingDistribution([9, 8, 7, 4, null, undefined]), {
      excellent: 2,
      good: 1,
      needsImprovement: 1,
      noData: 2,
    });
  });
});

describe('status maps', () => {
  it('covers every course run status the dashboard can return, including NONE', () => {
    assert.deepEqual(Object.keys(COURSE_RUN_STATUS).sort(), ['ACTIVE', 'CANCELLED', 'COMPLETED', 'NONE', 'UPCOMING']);
  });

});

describe('date ranges', () => {
  const today = new Date(2026, 9, 11); // 11 Oct 2026, local time

  it('formats a date in the local time zone, not UTC', () => {
    assert.equal(toIsoDate(new Date(2026, 0, 5, 0, 30)), '2026-01-05');
  });

  it('counts a preset back from today', () => {
    assert.deepEqual(rangeForPreset('7D', today), { from: '2026-10-04', to: '2026-10-11' });
    assert.deepEqual(rangeForPreset('30D', today), { from: '2026-09-11', to: '2026-10-11' });
  });

  it('has no bounds for all time and uses the typed dates for a custom range', () => {
    assert.deepEqual(rangeForPreset('ALL', today), {});
    assert.deepEqual(rangeForPreset('CUSTOM', today, { from: '2026-01-01', to: '' }), { from: '2026-01-01', to: undefined });
  });

  it('suggests tomorrow as the first training day, across a month end', () => {
    assert.equal(tomorrowIso(new Date(2026, 9, 31)), '2026-11-01');
  });
});

describe('schedule timeline', () => {
  it('reads backend times with or without seconds', () => {
    assert.equal(minutesOf('06:30'), 390);
    assert.equal(minutesOf('06:30:00'), 390);
  });

  it('offers only quarter-hour starts that keep the session inside the window', () => {
    const options = sessionStartOptions('06:00:00', '10:00:00', 60, 15);
    assert.equal(options[0], '06:00');
    assert.equal(options[1], '06:15');
    assert.equal(options.at(-1), '09:00');
    assert.equal(clockOf(405), '06:45');
  });
});

describe('shiftIsoDate', () => {
  it('crosses month and year ends', () => {
    assert.equal(shiftIsoDate('2026-10-31', 1), '2026-11-01');
    assert.equal(shiftIsoDate('2027-01-01', -1), '2026-12-31');
  });
});

describe('range label', () => {
  const format = (date: string) => `<${date}>`;

  it('names a preset', () => {
    assert.equal(rangeLabel('30D', {}, format), 'Last 30 days');
    assert.equal(rangeLabel('ALL', {}, format), 'All time');
  });

  it('spells out a custom range, open ended on one side', () => {
    assert.equal(rangeLabel('CUSTOM', { from: '2026-01-01', to: '2026-02-01' }, format), '<2026-01-01> – <2026-02-01>');
    assert.equal(rangeLabel('CUSTOM', { from: '2026-01-01' }, format), 'From <2026-01-01>');
    assert.equal(rangeLabel('CUSTOM', { to: '2026-02-01' }, format), 'Until <2026-02-01>');
    assert.equal(rangeLabel('CUSTOM', {}, format), 'All time');
  });
});
