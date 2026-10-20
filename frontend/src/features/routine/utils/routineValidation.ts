import type { CareSchedule } from '@/features/admissions/types';

export function getMinRoutineDate(): string {
  const now = new Date();
  // Backend CareScheduleService sets scheduled time to 14:00 on scheduledDate.
  // If local time is past 13:45, a same-day schedule would be rejected with 400 INVALID_DATE.
  if (now.getHours() > 13 || (now.getHours() === 13 && now.getMinutes() >= 45)) {
    now.setDate(now.getDate() + 1);
  }
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function validateCreateRoutineForm(data: {
  horseId: number;
  scheduledDate: string;
  description: string;
  todayDateStr?: string;
}): Record<string, string> {
  const errors: Record<string, string> = {};

  if (!data.horseId || data.horseId <= 0) {
    errors.horseId = 'Please select a horse';
  }

  if (!data.scheduledDate) {
    errors.scheduledDate = 'Please select a scheduled date';
  } else {
    const minDate = data.todayDateStr || getMinRoutineDate();
    if (data.scheduledDate < minDate) {
      errors.scheduledDate = 'Scheduled date cannot be in the past';
    }
  }

  if (!data.description || !data.description.trim()) {
    errors.description = 'Clinical reason is required';
  }

  return errors;
}

export function filterRoutineSchedules(
  schedules: CareSchedule[],
  statusFilter: string,
  searchTerm: string,
  horseMap: Map<number, { name: string; stallCode?: string }>,
): CareSchedule[] {
  const normalizedSearch = searchTerm.trim().toLowerCase();

  return schedules.filter((s) => {
    if (statusFilter !== 'ALL' && s.status !== statusFilter) {
      return false;
    }

    if (!normalizedSearch) {
      return true;
    }

    const horse = horseMap.get(s.horseId);
    const horseName = horse?.name.toLowerCase() || '';
    const stallCode = horse?.stallCode?.toLowerCase() || '';
    const desc = (s.description || '').toLowerCase();
    const idStr = String(s.id);

    return (
      horseName.includes(normalizedSearch) ||
      stallCode.includes(normalizedSearch) ||
      desc.includes(normalizedSearch) ||
      idStr.includes(normalizedSearch)
    );
  });
}
