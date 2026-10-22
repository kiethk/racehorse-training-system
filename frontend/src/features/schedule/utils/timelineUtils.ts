import type { CareType } from '../../admissions/types/index.ts';

// Mirrors FarmSchedulePolicy.VET_WINDOW_START / VET_WINDOW_END on the backend.
export const VET_WINDOW_START = 13 * 60 + 30; // 13:30 (810 mins)
export const VET_WINDOW_END = 15 * 60 + 30;   // 15:30 (930 mins)
export const VET_WINDOW_LEN = VET_WINDOW_END - VET_WINDOW_START; // 120 mins

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

/** Ruler ticks for the daily vet window, derived from the window constants. */
export function timelineRulerMarks(stepMinutes = 30): TimelineRulerMark[] {
  const marks: TimelineRulerMark[] = [];
  for (let minutes = VET_WINDOW_START; minutes <= VET_WINDOW_END; minutes += stepMinutes) {
    marks.push({
      minutes,
      label: formatClockLabel(minutes),
      percent: ((minutes - VET_WINDOW_START) / VET_WINDOW_LEN) * 100,
    });
  }
  return marks;
}

export function computeTimelinePosition(
  scheduledAt: string,
  durationMinutes: number = 30,
  windowStart: number = VET_WINDOW_START,
  windowLen: number = VET_WINDOW_LEN,
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

export function getCareTypeTone(careType: CareType) {
  switch (careType) {
    case 'URGENT':
      return {
        bg: 'bg-[var(--color-danger-soft)]',
        border: 'border-[var(--color-danger)]',
        text: 'text-[var(--color-danger)]',
        pillTone: 'danger' as const,
        icon: 'alert-triangle' as const,
        label: 'Urgent',
      };
    case 'INITIAL':
      return {
        bg: 'bg-[var(--color-info-soft)]',
        border: 'border-[var(--color-info)]',
        text: 'text-[var(--color-info)]',
        pillTone: 'info' as const,
        icon: 'stethoscope' as const,
        label: 'Initial',
      };
    case 'ROUTINE':
    default:
      return {
        bg: 'bg-[var(--color-surface-muted)]',
        border: 'border-[var(--color-border-strong)]',
        text: 'text-[var(--color-text-secondary)]',
        pillTone: 'neutral' as const,
        icon: 'clock' as const,
        label: 'Routine',
      };
  }
}
