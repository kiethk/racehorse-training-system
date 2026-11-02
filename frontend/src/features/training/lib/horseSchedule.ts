import type { HorseScheduleItem } from '../types';
import { shiftIsoDate } from './dateRange.ts';
import type { SessionState } from './status.ts';

/** Days shown at once in a horse's schedule. */
export const SCHEDULE_DAYS = 7;

/**
 * Cancelled if the session or this horse's own workout was cancelled; held once the session took place (or its time is over);
 * otherwise still upcoming.
 */
export function sessionState(item: HorseScheduleItem, now: Date): SessionState {
  if (item.sessionStatus === 'CANCELLED' || item.workoutStatus === 'CANCELLED') return 'CANCELLED';
  if (item.sessionStatus === 'HELD' || item.workoutStatus === 'HELD' || item.workoutStatus === 'RAN') return 'HELD';
  return new Date(`${item.sessionDate}T${item.endTime}`).getTime() <= now.getTime() ? 'HELD' : 'UPCOMING';
}

/** The seven days that start at `start`, as YYYY-MM-DD. */
export function scheduleDates(start: string): string[] {
  return Array.from({ length: SCHEDULE_DAYS }, (_, index) => shiftIsoDate(start, index));
}

/** Sessions grouped by day, each day in time order. Days without a session are simply absent. */
export function groupByDate(items: HorseScheduleItem[]): Map<string, HorseScheduleItem[]> {
  const byDate = new Map<string, HorseScheduleItem[]>();
  for (const item of items) {
    byDate.set(item.sessionDate, [...(byDate.get(item.sessionDate) ?? []), item]);
  }
  for (const sessions of byDate.values()) sessions.sort((a, b) => a.startTime.localeCompare(b.startTime));
  return byDate;
}
