import type { CourseRunStatus } from '../types';

export interface CourseRunProgress {
  completed: number;
  /** Sessions that still count: cancelled ones are removed unless the whole course run was cancelled. */
  total: number;
  cancelled: number;
  percent: number;
  /** Every session was cancelled: show that instead of an empty 0/0 bar. */
  allCancelled: boolean;
}

/**
 * Progress of a course run. Cancelled sessions leave the denominator, so a 12-session course run with 1 cancelled reaches 100% at
 * 11 completed sessions instead of hanging at 91% forever. A CANCELLED course run keeps them: it was dropped part-way, so
 * 3 held of 6 reads 3/6 (50%), not a misleading 3/3 (100%).
 */
export function courseRunProgress(
  completed: number,
  planned: number,
  cancelled: number,
  status: CourseRunStatus | 'NONE',
): CourseRunProgress {
  const total = status === 'CANCELLED' ? planned : Math.max(planned - cancelled, 0);
  const percent = total === 0 ? 0 : Math.min(Math.round((completed / total) * 100), 100);
  return { completed, total, cancelled, percent, allCancelled: completed === 0 && cancelled > 0 && cancelled >= planned };
}

/** "1 session cancelled" / "3 sessions cancelled". */
export function cancelledLabel(cancelled: number): string {
  return `${cancelled} ${cancelled === 1 ? 'session' : 'sessions'} cancelled`;
}
