import type { CareType } from '../../admissions/types/index.ts';

// Mirrors FarmSchedulePolicy.VET_WINDOW_START / VET_WINDOW_END on the backend.
export const VET_WINDOW_START = 13 * 60 + 30; // 13:30 (810 mins) — the routine care window
export const VET_WINDOW_END = 15 * 60 + 30;   // 15:30 (930 mins)
export const VET_WINDOW_LEN = VET_WINDOW_END - VET_WINDOW_START; // 120 mins

// The vet's working day, mirroring the backend assignment grid (8:00 - 17:00).
// Routine exams prefer the window above; Admission exams fill the day outside it first.
export const WORKDAY_START = 8 * 60;   // 08:00 (480 mins)
export const WORKDAY_END = 17 * 60;    // 17:00 (1020 mins)
export const WORKDAY_LEN = WORKDAY_END - WORKDAY_START; // 540 mins

/** "13:30" from minutes since midnight. */
export function formatClockLabel(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(rest).padStart(2, '0')}`;
}

export interface TimelineRulerMark {
  minutes: number;
  label: string;
  percent: number;
}

/** Hour ticks for the working day, derived from the workday constants. */
export function timelineRulerMarks(stepMinutes = 60): TimelineRulerMark[] {
  const marks: TimelineRulerMark[] = [];
  for (let minutes = WORKDAY_START; minutes <= WORKDAY_END; minutes += stepMinutes) {
    marks.push({
      minutes,
      label: formatClockLabel(minutes),
      percent: ((minutes - WORKDAY_START) / WORKDAY_LEN) * 100,
    });
  }
  return marks;
}

export function computeTimelinePosition(
  scheduledAt: string,
  durationMinutes: number = 30,
  windowStart: number = WORKDAY_START,
  windowLen: number = WORKDAY_LEN,
) {
  const date = new Date(scheduledAt);
  if (Number.isNaN(date.getTime())) {
    return { leftPercent: 0, widthPercent: 0 };
  }

  const mins = date.getHours() * 60 + date.getMinutes();
  const leftPercent = Math.max(0, Math.min(100, ((mins - windowStart) / windowLen) * 100));
  const widthPercent = Math.max(2, Math.min(100 - leftPercent, (durationMinutes / windowLen) * 100));

  return {
    leftPercent: Number(leftPercent.toFixed(2)),
    widthPercent: Number(widthPercent.toFixed(2)),
  };
}

/** True when the exam falls outside the 08:00-17:00 display range (typically urgent cases). */
export function isOutsideWorkday(scheduledAt: string): boolean {
  const date = new Date(scheduledAt);
  if (Number.isNaN(date.getTime())) return false;
  const mins = date.getHours() * 60 + date.getMinutes();
  return mins < WORKDAY_START || mins >= WORKDAY_END;
}

/**
 * Greedy interval partition for timeline blocks: overlapping cases get separate lanes so every
 * block stays visible and clickable. Blocks that merely touch (end == next start) share a lane.
 */
export function assignTimelineLanes(items: { id: number; start: number; end: number }[]): {
  lanes: number;
  laneOf: Map<number, number>;
} {
  const laneEnds: number[] = [];
  const laneOf = new Map<number, number>();
  for (const item of [...items].sort((a, b) => a.start - b.start || a.end - b.end)) {
    let lane = laneEnds.findIndex((end) => item.start >= end);
    if (lane === -1) lane = laneEnds.length;
    laneEnds[lane] = item.end;
    laneOf.set(item.id, lane);
  }
  return { lanes: Math.max(1, laneEnds.length), laneOf };
}

export interface CareTypeTone {
  bg: string;
  border: string;
  text: string;
  pillTone: 'danger' | 'info' | 'neutral' | 'primary';
  icon: 'alert-triangle' | 'stethoscope' | 'clock' | 'pill' | 'clipboard';
  label: string;
}

/**
 * Single source of truth for examination tags across the vet screens.
 * `purpose` refines ROUTINE into Preventive / Follow-up; INITIAL is the admission exam.
 */
export function getCareTypeTone(careType: CareType, purpose?: string | null): CareTypeTone {
  switch (careType) {
    case 'URGENT':
      return {
        bg: 'bg-[var(--color-danger-soft)]',
        border: 'border-[var(--color-danger)]',
        text: 'text-[var(--color-danger)]',
        pillTone: 'danger',
        icon: 'alert-triangle',
        label: 'Urgent',
      };
    case 'INITIAL':
      return {
        bg: 'bg-[var(--color-info-soft)]',
        border: 'border-[var(--color-info)]',
        text: 'text-[var(--color-info)]',
        pillTone: 'info',
        icon: 'stethoscope',
        label: 'Admission',
      };
    case 'ROUTINE':
    default:
      if (purpose === 'PREVENTIVE') {
        return {
          bg: 'bg-[var(--color-primary-soft)]',
          border: 'border-[var(--color-primary)]',
          text: 'text-[var(--color-primary)]',
          pillTone: 'primary',
          icon: 'pill',
          label: 'Preventive',
        };
      }
      if (purpose === 'FOLLOW_UP') {
        return {
          bg: 'bg-[var(--color-surface-muted)]',
          border: 'border-[var(--color-border-strong)]',
          text: 'text-[var(--color-text-secondary)]',
          pillTone: 'neutral',
          icon: 'clipboard',
          label: 'Follow-up',
        };
      }
      return {
        bg: 'bg-[var(--color-surface-muted)]',
        border: 'border-[var(--color-border-strong)]',
        text: 'text-[var(--color-text-secondary)]',
        pillTone: 'neutral',
        icon: 'clock',
        label: 'Routine',
      };
  }
}
