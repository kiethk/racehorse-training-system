import type { CareSchedule } from '../../admissions/types/index.ts';
import type { VetWatchlistHorse } from '../types.ts';
import { getSynchronizedNow } from '../../../lib/clockSync.ts';

/** Local calendar date (YYYY-MM-DD) of "now" — the vet works in Asia/Ho_Chi_Minh, the app clock is local. */
export function getTodayLocalDate(): string {
  const now = getSynchronizedNow();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

/**
 * Filters urgent schedules to return active (pending/in-progress/overdue) emergency cases.
 * Excludes COMPLETED schedules (AD-022: CANCELLED has been abolished).
 */
export function filterActiveEmergencies(schedules: CareSchedule[]): CareSchedule[] {
  return schedules.filter(
    (s) => s.careType === 'URGENT' && (s.status === 'SCHEDULED' || s.status === 'IN_PROGRESS')
  );
}

/**
 * Filters horses who have a 'BLOCKED' training decision, mapping them to the watchlist view structure.
 * Horses rejected from the club are excluded: they keep their BLOCKED decision but no longer belong on the watchlist.
 */
export function filterBlockedHorses(
  horses: Array<{
    id: number;
    name: string;
    breed?: string | null;
    currentStallId?: number | null;
    currentStatus?: string;
    trainingDecision?: string;
    trainingDecisionReason?: string | null;
  }>,
  stallMap: Map<number, string>
): VetWatchlistHorse[] {
  return horses
    .filter((h) => h.trainingDecision === 'BLOCKED' && h.currentStatus !== 'REJECTED')
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
      if (s.careType === 'URGENT') return false;
      if (s.scheduledAt) {
        return s.scheduledAt.startsWith(targetDateYMD);
      }
      return s.scheduledDate === targetDateYMD;
    })
    .sort((a, b) => (a.scheduledAt || '').localeCompare(b.scheduledAt || ''));
}

/** One week of the vet's activity chart. Only weeks with at least one completed exam appear. */
export interface VetWeekActivity {
  /** The Monday of the week, YYYY-MM-DD. */
  weekStart: string;
  completed: number;
  /** Completed exams that were URGENT cases. */
  urgent: number;
  /** Exams still in progress that week — shown in the tooltip so a past week reads honestly. */
  inProgress: number;
}

/** The Monday (YYYY-MM-DD) of the week containing a local date, without timezone drift. */
function mondayOf(dateYmd: string): string {
  const [year, month, day] = dateYmd.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7));
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

const EXAM_ACTIVITY_WEEKS = 12;

/**
 * Buckets the viewer's care schedules into the last 12 weeks for the dashboard activity chart.
 * A week's bar is the number of COMPLETED exams (the work actually delivered); URGENT completions
 * and still-open exams ride along for the tooltip. CANCELLED and not-yet-held exams draw nothing.
 */
export function examActivityByWeek(
  schedules: CareSchedule[],
  todayYmd: string
): { fromDate: string; toDate: string; items: VetWeekActivity[] } {
  const thisMonday = mondayOf(todayYmd);
  const fromDate = mondayOf(isoDateMinusDays(thisMonday, 7 * (EXAM_ACTIVITY_WEEKS - 1)));

  const byWeek = new Map<string, VetWeekActivity>();
  for (const schedule of schedules) {
    const dateKey = schedule.scheduledAt?.slice(0, 10) || schedule.scheduledDate;
    if (!dateKey) continue;
    const week = mondayOf(dateKey);
    if (week < fromDate || week > thisMonday) continue;

    let entry = byWeek.get(week);
    if (!entry) {
      entry = { weekStart: week, completed: 0, urgent: 0, inProgress: 0 };
      byWeek.set(week, entry);
    }
    if (schedule.status === 'COMPLETED') {
      entry.completed += 1;
      if (schedule.careType === 'URGENT') entry.urgent += 1;
    } else if (schedule.status === 'IN_PROGRESS') {
      entry.inProgress += 1;
    }
  }

  const items = [...byWeek.values()]
    .filter((week) => week.completed > 0)
    .sort((a, b) => a.weekStart.localeCompare(b.weekStart));
  return { fromDate, toDate: todayYmd, items };
}

/** Subtract days from a YYYY-MM-DD date the same TZ-safe way mondayOf parses. */
function isoDateMinusDays(dateYmd: string, days: number): string {
  const [year, month, day] = dateYmd.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  date.setDate(date.getDate() - days);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}
