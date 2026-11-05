import test from 'node:test';
import assert from 'node:assert/strict';
import { validateCreateRoutineForm, filterRoutineSchedules } from '../routineValidation.ts';
import type { CareSchedule } from '../../../admissions/types/index.ts';

test('validateCreateRoutineForm validates required horse, date and description', () => {
  const today = '2026-10-18';

  // Empty horseId
  const err1 = validateCreateRoutineForm({ horseId: 0, scheduledDate: '2026-10-20', description: 'Checkup', todayDateStr: today });
  assert.equal(err1.horseId, 'Please select a horse');

  // Negative horseId
  const err1b = validateCreateRoutineForm({ horseId: -1, scheduledDate: '2026-10-20', description: 'Checkup', todayDateStr: today });
  assert.equal(err1b.horseId, 'Please select a horse');

  // Missing scheduledDate
  const err2a = validateCreateRoutineForm({ horseId: 5, scheduledDate: '', description: 'Checkup', todayDateStr: today });
  assert.equal(err2a.scheduledDate, 'Please select a scheduled date');

  // Past date
  const err2 = validateCreateRoutineForm({ horseId: 5, scheduledDate: '2026-10-10', description: 'Checkup', todayDateStr: today });
  assert.equal(err2.scheduledDate, 'Scheduled date cannot be in the past');

  // Today date is allowed
  const errToday = validateCreateRoutineForm({ horseId: 5, scheduledDate: '2026-10-18', description: 'Checkup', todayDateStr: today });
  assert.equal(errToday.scheduledDate, undefined);

  // Empty description
  const err3 = validateCreateRoutineForm({ horseId: 5, scheduledDate: '2026-10-20', description: '   ', todayDateStr: today });
  assert.equal(err3.description, 'Clinical reason is required');

  // Valid form
  const err4 = validateCreateRoutineForm({ horseId: 5, scheduledDate: '2026-10-20', description: 'Valid reason', todayDateStr: today });
  assert.equal(Object.keys(err4).length, 0);
});

test('filterRoutineSchedules correctly filters by status and search keyword', () => {
  const schedules: CareSchedule[] = [
    { id: 1, horseId: 10, careType: 'ROUTINE', status: 'SCHEDULED', description: 'Deworming treatment' },
    { id: 2, horseId: 20, careType: 'ROUTINE', status: 'IN_PROGRESS', description: 'Post-injury checkup' },
    { id: 3, horseId: 10, careType: 'ROUTINE', status: 'COMPLETED', description: 'Vaccine rabies' },
  ];
  const horseMap = new Map<number, { name: string; stallCode?: string }>([
    [10, { name: 'Thunderbolt', stallCode: 'A-01' }],
    [20, { name: 'Shadowfax', stallCode: 'B-02' }],
  ]);

  // Filter by status SCHEDULED
  const res1 = filterRoutineSchedules(schedules, 'SCHEDULED', '', horseMap);
  assert.equal(res1.length, 1);
  assert.equal(res1[0].id, 1);

  // Search by horse name
  const res2 = filterRoutineSchedules(schedules, 'ALL', 'shadow', horseMap);
  assert.equal(res2.length, 1);
  assert.equal(res2[0].id, 2);

  // Search by description keyword
  const res3 = filterRoutineSchedules(schedules, 'ALL', 'vaccine', horseMap);
  assert.equal(res3.length, 1);
  assert.equal(res3[0].id, 3);

  // Search by stall code
  const res4 = filterRoutineSchedules(schedules, 'ALL', 'a-01', horseMap);
  assert.equal(res4.length, 2);

  // Search by ID
  const res5 = filterRoutineSchedules(schedules, 'ALL', '2', horseMap);
  assert.equal(res5.length, 1);
  assert.equal(res5[0].id, 2);

  // Search non-matching term
  const res6 = filterRoutineSchedules(schedules, 'ALL', 'nonexistent', horseMap);
  assert.equal(res6.length, 0);

  // Handles schedule without matching horse in horseMap and without description
  const unmappedSchedules: CareSchedule[] = [
    { id: 99, horseId: 999, careType: 'ROUTINE', status: 'SCHEDULED', description: null },
  ];
  const res7 = filterRoutineSchedules(unmappedSchedules, 'SCHEDULED', '', horseMap);
  assert.equal(res7.length, 1);
  const res8 = filterRoutineSchedules(unmappedSchedules, 'SCHEDULED', '99', horseMap);
  assert.equal(res8.length, 1);
});
