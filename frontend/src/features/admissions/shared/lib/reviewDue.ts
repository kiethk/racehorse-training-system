const HOUR_MS = 60 * 60 * 1000;

export const GROOM_FULL_MESSAGE = 'All your quarantine stalls are taken. You can reject this admission or wait for a stall to free up.';

export function reviewDueTone(dueAt: string | null, now = Date.now()): 'neutral' | 'warning' | 'danger' {
  const remaining = dueAt ? new Date(dueAt).getTime() - now : NaN;
  if (remaining < 0) return 'danger';
  if (remaining < 12 * HOUR_MS) return 'warning';
  return 'neutral';
}

export function reviewDueRelative(dueAt: string | null, now = Date.now()): string | null {
  const remaining = dueAt ? new Date(dueAt).getTime() - now : NaN;
  if (!Number.isFinite(remaining)) return null;
  const minutes = Math.ceil(Math.abs(remaining) / 60000);
  const duration = minutes < 60 ? `${minutes} min` : `${Math.ceil(minutes / 60)} h`;
  return remaining < 0 ? `${duration} overdue` : `in ${duration}`;
}
