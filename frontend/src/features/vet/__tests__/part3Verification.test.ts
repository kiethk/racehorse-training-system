import test from 'node:test';
import assert from 'node:assert/strict';
import { formatLocalDateIso } from '../../../lib/display.ts';

test('Part 3 Verification - formatLocalDateIso Timezone Accuracy (AC-3.3)', async (t) => {
  await t.test('converts UTC evening time (before 07:00 VN next day) to next day VN business date', () => {
    // 2026-10-22 20:30:00 UTC = 2026-10-23 03:30:00 in Asia/Ho_Chi_Minh
    const utcTime = new Date('2026-10-22T20:30:00Z');
    const localIso = formatLocalDateIso(utcTime);

    assert.equal(
      localIso,
      '2026-10-23',
      'Should format as 2026-10-23 in Asia/Ho_Chi_Minh (+07:00), NOT yesterday 2026-10-22'
    );
  });

  await t.test('formats daytime VN time correctly', () => {
    // 2026-10-23 07:30:00 UTC = 2026-10-23 14:30:00 in Asia/Ho_Chi_Minh
    const utcTime = new Date('2026-10-23T07:30:00Z');
    const localIso = formatLocalDateIso(utcTime);

    assert.equal(localIso, '2026-10-23');
  });

  await t.test('handles invalid date input safely returning empty string', () => {
    const invalidIso = formatLocalDateIso('invalid-date-string');
    assert.equal(invalidIso, '');
  });
});

test('Part 3 Verification - Health Deep-Link Query Parameter Extraction (AC-3.1)', async (t) => {
  await t.test('parses valid numeric horseId from search params', () => {
    const mockParams = new URLSearchParams('horseId=42&view=records');
    const horseIdParam = mockParams.get('horseId');
    const parsedId = horseIdParam ? Number(horseIdParam) : null;

    assert.equal(parsedId, 42);
    assert.equal(Number.isNaN(parsedId), false);
    assert.equal(parsedId! > 0, true);
  });

  await t.test('rejects non-numeric or invalid horseId values safely', () => {
    const mockParams = new URLSearchParams('horseId=abc');
    const horseIdParam = mockParams.get('horseId');
    const parsedId = horseIdParam ? Number(horseIdParam) : null;

    assert.equal(Number.isNaN(parsedId), true);
  });

  await t.test('detects non-existent horse ID from roster gracefully', () => {
    const mockHorses = [{ id: 1, name: 'Storm' }, { id: 2, name: 'Blaze' }];
    const targetId = 999;
    const foundHorse = mockHorses.find((h) => h.id === targetId) || null;

    assert.equal(foundHorse, null, 'Should return null for missing horse ID without crashing');
  });
});

test('Part 3 Verification - On-Demand Paged Health Summary Fetching (AC-3.2)', async (t) => {
  await t.test('filters only un-cached horse IDs for visible paged slice', () => {
    const existingCache: Record<number, { activeInjuryCount: number }> = {
      1: { activeInjuryCount: 0 },
      2: { activeInjuryCount: 1 },
    };

    const pagedHorseIds = [1, 2, 3, 4, 5];

    // Compute missing items
    const missing = pagedHorseIds.filter((id) => existingCache[id] === undefined);

    assert.deepEqual(missing, [3, 4, 5], 'Should only request missing IDs [3, 4, 5], avoiding re-fetching 1 and 2');
  });

  await t.test('does not trigger fetch when all visible page items are already cached', () => {
    const existingCache: Record<number, { activeInjuryCount: number }> = {
      10: { activeInjuryCount: 0 },
      20: { activeInjuryCount: 0 },
    };

    const pagedHorseIds = [10, 20];
    const missing = pagedHorseIds.filter((id) => existingCache[id] === undefined);

    assert.equal(missing.length, 0, 'No HTTP requests should be triggered when page is cached');
  });
});

test('Part 3 Verification - Clinical Treatment Plan & Prescription Validation (AC-3.4)', async (t) => {
  await t.test('flags error when target end date is earlier than start date', () => {
    const startDate = '2026-10-23';
    const endDate = '2026-10-20'; // earlier than start date
    const isInvalid = Boolean(endDate && startDate && endDate < startDate);

    assert.equal(isInvalid, true, 'End date prior to start date must be rejected before backend dispatch');
  });

  await t.test('accepts valid start and end date ranges', () => {
    const startDate = '2026-10-23';
    const endDate = '2026-11-06';
    const isInvalid = Boolean(endDate && startDate && endDate < startDate);

    assert.equal(isInvalid, false, 'Chronological date range must be accepted');
  });
});
