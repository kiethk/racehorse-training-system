import type { Role } from '@/types/auth';
import type { NotificationItem } from '../types';

/** Where "view details" on a notification should take the current user, if anywhere. */
export function getNotificationHref(item: NotificationItem, role: Role | undefined): string | null {
  const refId = item.referenceId;
  const isUrgent = item.title?.toLowerCase().includes('urgent') || item.message?.toLowerCase().includes('urgent');

  if (role === 'VETERINARIAN' && item.referenceType === 'CARE_SCHEDULE') {
    if (item.notificationType === 'CARE_SCHEDULE_URGENT_ASSIGNED') {
      return `/veterinarian/urgent/${refId}`;
    }
    if (item.notificationType === 'ROUTINE_ASSIGNED') {
      return '/veterinarian/routine';
    }
    if (item.notificationType === 'ADMISSION_VET_ASSIGNED') {
      // Legacy rows reused this type for INITIAL (admission quarantine) exams; the id is a
      // care-schedule id, so the admission queue - not a schedule detail page - is the target.
      return '/veterinarian/admissions';
    }
    return null;
  }

  // 1. Phân công khám nhập chuồng (Admission Assignment)
  if (item.notificationType === 'ADMISSION_VET_ASSIGNED') {
    return `/veterinarian/admissions`;
  }
  if (item.notificationType === 'ADMISSION_TRAINER_ASSIGNED') {
    return refId ? `/trainer/admissions/${refId}` : `/trainer/admissions`;
  }
  if (item.referenceType === 'ADMISSION') {
    if (role === 'VETERINARIAN') return `/veterinarian/admissions`;
    if (role === 'HEAD_TRAINER') return refId ? `/trainer/admissions/${refId}` : `/trainer/admissions`;
  }

  // 2. Lịch chăm sóc y tế (Care Schedule)
  if (item.referenceType === 'CARE_SCHEDULE' && role === 'VETERINARIAN') {
    if (isUrgent && refId) {
      return `/veterinarian/urgent/${refId}`;
    }
    return `/veterinarian/preventive-care`;
  }

  // 3. Huấn luyện viên
  if (role === 'HEAD_TRAINER') {
    return `/trainer/schedule`;
  }

  // 4. Groom
  if (role === 'GROOM') {
    return `/groom/tasks`;
  }

  return null;
}
