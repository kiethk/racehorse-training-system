import type { CourseRunStatus, SessionHorseOutcome, SessionStatus, WorkoutStatus } from '../types';

export type StatusTone = 'primary' | 'success' | 'info' | 'warning' | 'danger' | 'neutral';

export interface StatusStyle {
  label: string;
  tone: StatusTone;
}

/** One status-to-tone map for training course runs, so the list, the detail and the dashboard agree. */
export const COURSE_RUN_STATUS: Record<CourseRunStatus | 'NONE', StatusStyle> = {
  UPCOMING: { label: 'Upcoming', tone: 'info' },
  ACTIVE: { label: 'In training', tone: 'primary' },
  COMPLETED: { label: 'Completed', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
  NONE: { label: 'No course run', tone: 'neutral' },
};

export const WORKOUT_STATUS: Record<WorkoutStatus, StatusStyle> = {
  SCHEDULED: { label: 'Scheduled', tone: 'info' },
  HELD: { label: 'Needs result', tone: 'warning' },
  RAN: { label: 'Ran', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
};

/** What a session looks like in a horse's schedule: not yet held, held, or called off. */
export type SessionState = 'UPCOMING' | 'HELD' | 'CANCELLED';

export const SESSION_STATE: Record<SessionState, StatusStyle> = {
  UPCOMING: { label: 'Upcoming', tone: 'info' },
  HELD: { label: 'Held', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
};


/** A group Session: ahead, Held (its end time passed), or cancelled. Held follows the clock, not the results. */
export const SESSION_STATUS: Record<SessionStatus, StatusStyle> = {
  SCHEDULED: { label: 'Scheduled', tone: 'info' },
  HELD: { label: 'Held', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
};

/** What became of one horse in one Session of a course run. */
export const SESSION_HORSE_OUTCOME: Record<SessionHorseOutcome, StatusStyle> = {
  TO_RUN: { label: 'To run', tone: 'info' },
  NEEDS_RESULT: { label: 'Needs result', tone: 'warning' },
  RAN: { label: 'Ran', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
  DID_NOT_RUN: { label: 'Did not run', tone: 'warning' },
};

/**
 * What became of one horse in one Session, derived from its Workout status and its Session status: a cancelled Workout in a Session
 * that took place is "Did not run"; a cancelled one before that is "Cancelled".
 */
export function sessionHorseOutcome(workoutStatus: WorkoutStatus, sessionStatus: SessionStatus): SessionHorseOutcome {
  switch (workoutStatus) {
    case 'SCHEDULED':
      return 'TO_RUN';
    case 'HELD':
      return 'NEEDS_RESULT';
    case 'RAN':
      return 'RAN';
    case 'CANCELLED':
      return sessionStatus === 'HELD' ? 'DID_NOT_RUN' : 'CANCELLED';
  }
}
