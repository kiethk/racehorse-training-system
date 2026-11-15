import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { reviewDueTone, reviewDueRelative } from '../../shared/lib/reviewDue.ts';

const now = Date.parse('2026-11-06T00:00:00Z');
const at = (hours: number) => new Date(now + hours * 3600000).toISOString();

describe('Review due', () => {
  it('uses neutral at and beyond 12 hours', () => {
    assert.equal(reviewDueTone(at(12), now), 'neutral');
    assert.equal(reviewDueTone(at(72), now), 'neutral');
  });
  it('warns below 12 hours including the due instant', () => {
    assert.equal(reviewDueTone(at(11.99), now), 'warning');
    assert.equal(reviewDueTone(at(0), now), 'warning');
  });
  it('uses danger only after the due instant', () => {
    assert.equal(reviewDueTone(at(-0.001), now), 'danger');
  });
  it('handles missing and invalid due times', () => {
    for (const value of [null, 'invalid']) {
      assert.equal(reviewDueTone(value, now), 'neutral');
      assert.equal(reviewDueRelative(value, now), null);
    }
  });
  it('formats future and overdue durations', () => {
    assert.equal(reviewDueRelative(at(5), now), 'in 5 h');
    assert.equal(reviewDueRelative(at(-5), now), '5 h overdue');
    assert.equal(reviewDueRelative(at(0.5), now), 'in 30 min');
  });
});
