import type { HorseScheduleItem } from '../types';
import { shiftIsoDate } from './dateRange.ts';
import type { SessionState } from './status.ts';

/** Days shown at once in a horse's schedule. */
export const SCHEDULE_DAYS = 7;

/**
 * Cancelled if the lot or this horse's own workout was cancelled; completed once it was recorded or its time is over;
 * otherwise still upcoming.
 */
export function sessionState(item: HorseScheduleItem, now: Date): SessionState {
  if (item.lotStatus === 'CANCELLED' || item.workoutStatus === 'CANCELLED') return 'CANCELLED';
  if (item.lotStatus === 'COMPLETED' || item.workoutStatus === 'COMPLETED') return 'COMPLETED';
  return new Date(`${item.lotDate}T${item.endTime}`).getTime() <= now.getTime() ? 'COMPLETED' : 'UPCOMING';
}

/** The seven days that start at `start`, as YYYY-MM-DD. */
export function scheduleDates(start: string): string[] {
  return Array.from({ length: SCHEDULE_DAYS }, (_, index) => shiftIsoDate(start, index));
}

/** Sessions grouped by day, each day in time order. Days without a session are simply absent. */
export function groupByDate(items: HorseScheduleItem[]): Map<string, HorseScheduleItem[]> {
  const byDate = new Map<string, HorseScheduleItem[]>();
  for (const item of items) {
    byDate.set(item.lotDate, [...(byDate.get(item.lotDate) ?? []), item]);
  }
  for (const sessions of byDate.values()) sessions.sort((a, b) => a.startTime.localeCompare(b.startTime));
  return byDate;
}
