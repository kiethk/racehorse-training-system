export interface CourseRunProgress {
  completed: number;
  /** Every Session the course run was created with, cancelled ones included. */
  total: number;
  cancelled: number;
  percent: number;
  /** Every session was cancelled: show that instead of an empty 0/0 bar. */
  allCancelled: boolean;
}

/**
 * Progress of a course run, or of one horse in it. The total is every Session the course run was created with, so a cancelled
 * Session stays in it: a course run that closed with 2 of 10 cancelled reads 8/10 (80%), never full. The same formula serves a closed,
 * a cancelled and a running course run.
 */
export function courseRunProgress(completed: number, planned: number, cancelled: number): CourseRunProgress {
  const percent = planned === 0 ? 0 : Math.min(Math.round((completed / planned) * 100), 100);
  return { completed, total: planned, cancelled, percent, allCancelled: completed === 0 && cancelled > 0 && cancelled >= planned };
}

/** "1 session cancelled" / "3 sessions cancelled". */
export function cancelledLabel(cancelled: number): string {
  return `${cancelled} ${cancelled === 1 ? 'session' : 'sessions'} cancelled`;
}
