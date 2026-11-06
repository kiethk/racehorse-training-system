import type { IconName } from '@/components/ui/Icon';
import type { CareScheduleStatus } from '@/features/admissions/types';

type PillTone = 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral';

export interface ScheduleStatusDisplay {
  label: string;
  tone: PillTone;
  icon: IconName;
}

/**
 * Single source of truth for how a care-schedule status looks, shared by the vet Dashboard,
 * Schedule, Admission queue and exam header. Per AD-026:
 * - OVERDUE (red)    = the slot passed and the vet never opened the exam (vet's fault).
 * - DELAYED (amber)  = the vet is examining but it ran past its duration (not penalised).
 * Always read `status`: the derived `overdue` boolean cannot tell these two apart.
 */
const STATUS_DISPLAY: Record<CareScheduleStatus, ScheduleStatusDisplay> = {
  REQUESTED: { label: 'Requested', tone: 'warning', icon: 'clock' },
  SCHEDULED: { label: 'Scheduled', tone: 'neutral', icon: 'calendar' },
  IN_PROGRESS: { label: 'In Progress', tone: 'primary', icon: 'activity' },
  PAUSED: { label: 'Paused', tone: 'warning', icon: 'clock' },
  DELAYED: { label: 'Delayed', tone: 'warning', icon: 'clock' },
  OVERDUE: { label: 'Overdue', tone: 'danger', icon: 'alert-triangle' },
  COMPLETED: { label: 'Completed', tone: 'success', icon: 'check' },
};

const UNKNOWN_STATUS: ScheduleStatusDisplay = { label: 'Unknown', tone: 'neutral', icon: 'circle' };

export function getScheduleStatusDisplay(status: CareScheduleStatus | string | null | undefined): ScheduleStatusDisplay {
  return STATUS_DISPLAY[status as CareScheduleStatus] ?? UNKNOWN_STATUS;
}

export interface ScheduleExamAction {
  label: string;
  icon: IconName;
}

/** The primary action button for a schedule row/card, unified across all screens. */
export function getScheduleExamAction(status: CareScheduleStatus | string | null | undefined): ScheduleExamAction {
  if (status === 'COMPLETED') {
    return { label: 'View Record', icon: 'file-text' };
  }
  return { label: 'Examine', icon: 'activity' };
}
