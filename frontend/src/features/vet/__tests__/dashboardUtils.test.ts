import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  filterActiveEmergencies,
  filterBlockedHorses,
  filterTodayAppointments,
} from '../lib/dashboardUtils.ts';
import type { CareScheduleResponse } from '../types.ts';

test('filterActiveEmergencies includes only pending/in-progress URGENT schedules', () => {
  const mockSchedules = [
    { id: 1, careType: 'URGENT', status: 'REQUESTED' },
    { id: 2, careType: 'URGENT', status: 'IN_PROGRESS' },
    { id: 3, careType: 'URGENT', status: 'COMPLETED' },
    { id: 4, careType: 'URGENT', status: 'CANCELLED' },
    { id: 5, careType: 'ROUTINE', status: 'REQUESTED' },
  ] as CareScheduleResponse[];

  const active = filterActiveEmergencies(mockSchedules);
  assert.equal(active.length, 2);
  assert.deepEqual(active.map((s) => s.id), [1, 2]);
});

test('filterBlockedHorses returns horses with BLOCKED training decision', () => {
  const mockHorses = [
    { id: 10, name: 'Storm', breed: 'Thoroughbred', currentStallId: 1, trainingDecision: 'BLOCKED', trainingDecisionReason: 'Fetlock swelling' },
    { id: 11, name: 'Blaze', breed: 'Arabian', currentStallId: 2, trainingDecision: 'ALLOWED', trainingDecisionReason: null },
    { id: 12, name: 'Flash', breed: 'Quarter', currentStallId: null, trainingDecision: 'BLOCKED', trainingDecisionReason: 'Under quarantine' },
  ];
  const stallMap = new Map<number, string>([[1, 'Barn A · S01'], [2, 'Barn A · S02']]);

  const blocked = filterBlockedHorses(mockHorses, stallMap);
  assert.equal(blocked.length, 2);
  assert.equal(blocked[0].name, 'Storm');
  assert.equal(blocked[0].stallCode, 'Barn A · S01');
  assert.equal(blocked[0].reason, 'Fetlock swelling');
  assert.equal(blocked[1].stallCode, 'Unassigned');
});

test('filterTodayAppointments sorts chronologically and filters by target date', () => {
  const mockSchedules = [
    { id: 1, scheduledAt: '2026-10-22T14:30:00' },
    { id: 2, scheduledAt: '2026-10-22T13:30:00' },
    { id: 3, scheduledAt: '2026-10-23T13:30:00' },
  ] as CareScheduleResponse[];

  const today = filterTodayAppointments(mockSchedules, '2026-10-22');
  assert.equal(today.length, 2);
  assert.equal(today[0].id, 2); // 13:30 first
  assert.equal(today[1].id, 1); // 14:30 second
});
