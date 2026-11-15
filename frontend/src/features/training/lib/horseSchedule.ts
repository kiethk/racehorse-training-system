import { shiftIsoDate } from './dateRange.ts';

/** Days shown at once in a horse's schedule. */
export const SCHEDULE_DAYS = 7;

/** The seven days that start at `start`, as YYYY-MM-DD. */
export function scheduleDates(start: string): string[] {
  return Array.from({ length: SCHEDULE_DAYS }, (_, index) => shiftIsoDate(start, index));
}
