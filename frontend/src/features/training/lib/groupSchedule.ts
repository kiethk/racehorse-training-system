import type { SessionSummary } from '../types';
import { shiftIsoDate, toIsoDate } from './dateRange.ts';

export const WEEK_DAYS = 7;

/** The Monday of the week that holds an ISO date (the week the schedule opens on). */
export function mondayOf(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number);
  const weekday = new Date(year, month - 1, day).getDay();   // 0 is Sunday
  return shiftIsoDate(isoDate, -((weekday + 6) % 7));
}

/** The seven days of a week, as ISO dates, starting on `start`. */
export function weekDates(start: string): string[] {
  return Array.from({ length: WEEK_DAYS }, (_, index) => shiftIsoDate(start, index));
}

/** Sessions grouped by their date, each day ordered by start time. */
export function groupByDate(sessions: SessionSummary[]): Record<string, SessionSummary[]> {
  const days: Record<string, SessionSummary[]> = {};
  for (const session of sessions) {
    (days[session.date] ??= []).push(session);
  }
  for (const day of Object.values(days)) {
    day.sort((a, b) => a.startTime.localeCompare(b.startTime));
  }
  return days;
}

/** A Session has started once its start time has passed; from then on it can no longer be moved or cancelled. */
export function sessionHasStarted(session: Pick<SessionSummary, 'date' | 'startTime'>, now: Date = new Date()): boolean {
  const today = toIsoDate(now);
  if (session.date !== today) return session.date < today;
  const time = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:00`;
  return session.startTime.slice(0, 8) <= time;
}

/** How many Held Workouts across the Sessions still wait for a result. */
export function totalNeedingResult(sessions: SessionSummary[]): number {
  return sessions.reduce((sum, session) => sum + session.needsResultCount, 0);
}

/** "X of Y ran" for a horse's Attendance line: Workouts that Ran out of every session of the course run. */
export function attendanceLabel(attended: number, total: number): string {
  return `${attended} of ${total} ran`;
}
