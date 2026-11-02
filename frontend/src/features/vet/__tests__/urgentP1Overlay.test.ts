import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
  isOverlaySuppressed,
  getNextSnoozeExpiry,
  shouldPlayEmergencySound,
  selectP1OverlayAlert,
  DEFAULT_SNOOZE_MS,
} from '../lib/urgentOverlayUtils.ts';

describe('Urgent P1 Overlay Utility Tests', () => {
  const baseNow = 1730000000000;

  it('suppresses overlay when vet is already on the urgent case page', () => {
    const suppressed = isOverlaySuppressed(42, '/veterinarian/urgent/42', undefined, false, baseNow);
    assert.equal(suppressed, true);
  });

  it('does not suppress overlay when vet is on a different urgent case or other page', () => {
    const onOtherCase = isOverlaySuppressed(42, '/veterinarian/urgent/99', undefined, false, baseNow);
    assert.equal(onOtherCase, false);

    const onDashboard = isOverlaySuppressed(42, '/veterinarian', undefined, false, baseNow);
    assert.equal(onDashboard, false);
  });

  it('suppresses overlay when snooze has not expired', () => {
    const snoozedUntil = baseNow + 60000;
    const suppressed = isOverlaySuppressed(42, '/veterinarian', snoozedUntil, false, baseNow);
    assert.equal(suppressed, true);
  });

  it('allows overlay when snooze has expired', () => {
    const snoozedUntil = baseNow - 1000;
    const suppressed = isOverlaySuppressed(42, '/veterinarian', snoozedUntil, false, baseNow);
    assert.equal(suppressed, false);
  });

  it('suppresses overlay if user explicitly dismissed this case in current session', () => {
    const suppressed = isOverlaySuppressed(42, '/veterinarian', undefined, true, baseNow);
    assert.equal(suppressed, true);
  });

  it('calculates snooze expiry correctly', () => {
    const expiry = getNextSnoozeExpiry(baseNow, DEFAULT_SNOOZE_MS);
    assert.equal(expiry, baseNow + 10 * 60 * 1000);
  });

  it('respects prefersReducedMotion for emergency sound', () => {
    assert.equal(shouldPlayEmergencySound(true), false);
    assert.equal(shouldPlayEmergencySound(false), true);
  });

  it('selects the oldest eligible alert from a queue', () => {
    const alerts = [
      { scheduleId: 101, reportedAt: '2026-11-02T10:00:00Z' },
      { scheduleId: 102, reportedAt: '2026-11-02T09:30:00Z' }, // Older
      { scheduleId: 103, reportedAt: '2026-11-02T11:00:00Z' },
    ];

    const snoozedMap = new Map<number, number>();
    const dismissedSet = new Set<number>();

    const selected = selectP1OverlayAlert(alerts, '/veterinarian', snoozedMap, dismissedSet, baseNow);
    assert.notEqual(selected, null);
    assert.equal(selected?.scheduleId, 102);
  });

  it('skips snoozed or dismissed alerts when selecting P1 candidate', () => {
    const alerts = [
      { scheduleId: 101, reportedAt: '2026-11-02T09:00:00Z' },
      { scheduleId: 102, reportedAt: '2026-11-02T10:00:00Z' },
    ];

    const snoozedMap = new Map<number, number>([[101, baseNow + 50000]]);
    const dismissedSet = new Set<number>();

    const selected = selectP1OverlayAlert(alerts, '/veterinarian', snoozedMap, dismissedSet, baseNow);
    assert.notEqual(selected, null);
    assert.equal(selected?.scheduleId, 102);
  });

  it('returns null when all alerts are suppressed', () => {
    const alerts = [{ scheduleId: 101, reportedAt: '2026-11-02T09:00:00Z' }];
    const snoozedMap = new Map<number, number>();
    const dismissedSet = new Set<number>([101]);

    const selected = selectP1OverlayAlert(alerts, '/veterinarian', snoozedMap, dismissedSet, baseNow);
    assert.equal(selected, null);
  });
});
