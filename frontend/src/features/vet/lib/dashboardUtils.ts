import type { CareSchedule } from '../../admissions/types/index.ts';
import type { VetWatchlistHorse } from '../types.ts';

/**
 * Filters urgent schedules to return active (pending/in-progress) emergency cases.
 * Excludes COMPLETED and CANCELLED schedules.
 */
export function filterActiveEmergencies(schedules: CareSchedule[]): CareSchedule[] {
  return schedules.filter(
    (s) => s.careType === 'URGENT' && s.status !== 'COMPLETED' && s.status !== 'CANCELLED'
  );
}

/**
 * Filters horses who have a 'BLOCKED' training decision, mapping them to the watchlist view structure.
 */
export function filterBlockedHorses(
  horses: Array<{
    id: number;
    name: string;
    breed?: string | null;
    currentStallId?: number | null;
    trainingDecision?: string;
    trainingDecisionReason?: string | null;
  }>,
  stallMap: Map<number, string>
): VetWatchlistHorse[] {
  return horses
    .filter((h) => h.trainingDecision === 'BLOCKED')
    .map((h) => ({
      id: h.id,
      name: h.name,
      breed: h.breed,
      stallCode: (h.currentStallId != null ? stallMap.get(h.currentStallId) : null) || 'Unassigned',
      reason: h.trainingDecisionReason || 'Medical Hold',
    }));
}

/**
 * Filters care schedules for the target calendar date (YYYY-MM-DD)
 * and sorts them chronologically by scheduled time.
 */
export function filterTodayAppointments(
  schedules: CareSchedule[],
  targetDateYMD: string
): CareSchedule[] {
  return schedules
    .filter((s) => {
      if (s.scheduledAt) {
        return s.scheduledAt.startsWith(targetDateYMD);
      }
      return s.scheduledDate === targetDateYMD;
    })
    .sort((a, b) => (a.scheduledAt || '').localeCompare(b.scheduledAt || ''));
}
