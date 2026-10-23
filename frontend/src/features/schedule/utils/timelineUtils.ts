import type { CareType } from '../../admissions/types/index.ts';

export const VET_WINDOW_START = 8 * 60; // 08:00 (480 mins)
export const VET_WINDOW_END = 17 * 60;  // 17:00 (1020 mins)
export const VET_WINDOW_LEN = VET_WINDOW_END - VET_WINDOW_START; // 540 mins

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
        label: 'P1 · Urgent',
      };
    case 'INITIAL':
      return {
        bg: 'bg-[var(--color-info-soft)]',
        border: 'border-[var(--color-info)]',
        text: 'text-[var(--color-info)]',
        pillTone: 'info' as const,
        icon: 'stethoscope' as const,
        label: 'P2 · Initial',
      };
    case 'ROUTINE':
    default:
      return {
        bg: 'bg-[var(--color-surface-muted)]',
        border: 'border-[var(--color-border-strong)]',
        text: 'text-[var(--color-text-secondary)]',
        pillTone: 'neutral' as const,
        icon: 'clock' as const,
        label: 'P4 · Routine',
      };
  }
}
