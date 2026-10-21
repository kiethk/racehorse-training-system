import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import type { NotificationItem } from '../types.ts';
import { getNotificationHref } from '../lib/notificationRoute.ts';

function vetCareScheduleNotification(
  overrides: Pick<NotificationItem, 'notificationType' | 'referenceType' | 'referenceId' | 'title' | 'message'>,
): NotificationItem {
  return { id: 1, read: false, createdAt: '2026-10-21T00:00:00Z', ...overrides };
}

describe('Notification routing for veterinarian care-schedule notifications', () => {
  it('1. Urgent assignment links to the urgent case detail page', () => {
    const item = vetCareScheduleNotification({
      notificationType: 'CARE_SCHEDULE_URGENT_ASSIGNED',
      referenceType: 'CARE_SCHEDULE',
      referenceId: 42,
      title: 'Urgent case assigned',
      message: 'An urgent case has been assigned to you.',
    });

    assert.equal(getNotificationHref(item, 'VETERINARIAN'), '/veterinarian/urgent/42');
  });

  it('2. Routine assignment links to the routine schedule page', () => {
    const item = vetCareScheduleNotification({
      notificationType: 'ROUTINE_ASSIGNED',
      referenceType: 'CARE_SCHEDULE',
      referenceId: 7,
      title: 'Routine care assigned',
      message: 'A routine care schedule has been assigned to you.',
    });

    assert.equal(getNotificationHref(item, 'VETERINARIAN'), '/veterinarian/routine');
  });

  it('3. Admission (initial exam) assignment links to the admission queue', () => {
    const item = vetCareScheduleNotification({
      notificationType: 'ADMISSION_VET_ASSIGNED',
      referenceType: 'CARE_SCHEDULE',
      referenceId: 11,
      title: 'Admission exam assigned',
      message: 'An initial exam has been assigned to you.',
    });

    assert.equal(getNotificationHref(item, 'VETERINARIAN'), '/veterinarian/admissions');
  });

  it('4. Unknown care-schedule notification types have no link', () => {
    const item = vetCareScheduleNotification({
      notificationType: 'SOMETHING_ELSE',
      referenceType: 'CARE_SCHEDULE',
      referenceId: 3,
      title: 'Care schedule update',
      message: 'A care schedule has changed.',
    });

    assert.equal(getNotificationHref(item, 'VETERINARIAN'), null);
  });
});
