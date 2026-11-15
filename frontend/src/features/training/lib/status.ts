import type { CourseRunStatus, ListedWorkoutStatus, SessionStatus } from '../types';

export type StatusTone = 'primary' | 'success' | 'info' | 'warning' | 'danger' | 'neutral';

export interface StatusStyle {
  label: string;
  tone: StatusTone;
}

/** One status-to-tone map for training course runs, so the list, the detail and the dashboard agree. */
export const COURSE_RUN_STATUS: Record<CourseRunStatus | 'NONE', StatusStyle> = {
  UPCOMING: { label: 'Upcoming', tone: 'info' },
  ACTIVE: { label: 'In training', tone: 'primary' },
  COMPLETED: { label: 'Finished', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
  NONE: { label: 'No course run', tone: 'neutral' },
};

/** One horse in one Session: still to run, waiting for its result, or ran. Cancelled Workouts are never shown. */
export const WORKOUT_STATUS: Record<ListedWorkoutStatus, StatusStyle> = {
  SCHEDULED: { label: 'Scheduled', tone: 'info' },
  HELD: { label: 'Needs result', tone: 'warning' },
  RAN: { label: 'Ran', tone: 'success' },
};

/** A group Session: ahead, Held (its end time passed), or cancelled. Held follows the clock, not the results. */
export const SESSION_STATUS: Record<SessionStatus, StatusStyle> = {
  SCHEDULED: { label: 'Scheduled', tone: 'info' },
  HELD: { label: 'Held', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
};
