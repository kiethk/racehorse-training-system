import type { PlanStatus, SessionStatus, WorkoutStatus } from '../types';

export type StatusTone = 'primary' | 'success' | 'info' | 'warning' | 'danger' | 'neutral';

export interface StatusStyle {
  label: string;
  tone: StatusTone;
}

/** One status-to-tone map for training plans, so the list, the detail and the dashboard agree. */
export const PLAN_STATUS: Record<PlanStatus | 'NONE', StatusStyle> = {
  UPCOMING: { label: 'Upcoming', tone: 'info' },
  ACTIVE: { label: 'In training', tone: 'primary' },
  COMPLETED: { label: 'Completed', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'danger' },
  NONE: { label: 'No plan', tone: 'neutral' },
};

export const WORKOUT_STATUS: Record<WorkoutStatus, StatusStyle> = {
  SCHEDULED: { label: 'Scheduled', tone: 'info' },
  HELD: { label: 'Held', tone: 'success' },
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
