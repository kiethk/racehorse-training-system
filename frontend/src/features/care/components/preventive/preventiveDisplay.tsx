import { Pill } from '@/components/ui';
import type { PreventiveDueStatus, RescheduleReason } from '../../types/preventive';

type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export const DUE_STATUS_META: Record<PreventiveDueStatus, { label: string; tone: Tone }> = {
  OVERDUE: { label: 'Overdue', tone: 'danger' },
  MISSING: { label: 'No record', tone: 'danger' },
  DUE_SOON: { label: 'Due soon', tone: 'warning' },
  OK: { label: 'Up to date', tone: 'success' },
  NONE: { label: 'Not started', tone: 'neutral' },
};

export const RESCHEDULE_REASONS: { value: RescheduleReason; label: string }[] = [
  { value: 'UNDER_TREATMENT', label: 'Under treatment / unwell' },
  { value: 'RACE_CONFLICT', label: 'Clashes with a race or heavy training' },
  { value: 'OWNER_REQUEST', label: 'Owner request' },
  { value: 'WEATHER', label: 'Weather' },
  { value: 'OTHER', label: 'Other' },
];

/** Window in which a due dose is offered as a combined vaccination during another exam. */
export const OPPORTUNITY_WINDOW_DAYS = 7;
/** Above this temperature (°C) the exam form suggests deferring vaccination. */
export const FEVER_THRESHOLD_C = 38.5;

export function DueStatusPill({ status }: { status: PreventiveDueStatus }) {
  const meta = DUE_STATUS_META[status];
  return (
    <Pill tone={meta.tone} size="sm">
      {meta.label}
    </Pill>
  );
}

// Plain date helper, kept in a .ts file so logic tests can import it; re-exported for existing imports.
export { todayIso } from './preventiveDates';

export function addDaysIso(iso: string, days: number): string {
  const date = new Date(`${iso}T00:00:00`);
  date.setDate(date.getDate() + days);
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 10);
}

/** A dose is worth offering in this exam when it is overdue, missing or due within the window. */
export function isDueForCombinedVisit(status: PreventiveDueStatus, nextDueOn: string | null, today: string): boolean {
  if (status === 'OVERDUE' || status === 'MISSING') return true;
  return nextDueOn != null && nextDueOn <= addDaysIso(today, OPPORTUNITY_WINDOW_DAYS);
}
