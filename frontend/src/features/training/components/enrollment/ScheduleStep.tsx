import { Button, FieldLabel, FormField, Icon, Input, Panel, SectionTitle } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/display';
import type { PlanPreviewResponse, TrainingDay } from '../../types';

export const DAYS_OF_WEEK: { value: TrainingDay; label: string }[] = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
];

const DAY_WITH_DATE: Intl.DateTimeFormatOptions = { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' };

/**
 * Step 2: first training date and weekdays. The period the course will really cover comes from the backend preview,
 * because the end date depends on the session count and the weekdays together. The summary line shows the real first
 * session, so a start date that is not a training day needs no extra explanation.
 */
export function ScheduleStep({
  startDate,
  minDate,
  onStartDateChange,
  trainingDays,
  onToggleDay,
  preview,
  stale,
}: {
  startDate: string;
  /** Today: a plan cannot start in the past. */
  minDate: string;
  onStartDateChange: (value: string) => void;
  trainingDays: TrainingDay[];
  onToggleDay: (day: TrainingDay) => void;
  preview: PlanPreviewResponse | null;
  /** The preview belongs to the previous schedule and is being refreshed. */
  stale: boolean;
}) {
  const inPast = startDate !== '' && startDate < minDate;

  return (
    <Panel padded className="space-y-4">
      <SectionTitle>2. Schedule and training days</SectionTitle>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <FormField
          label="Start date"
          required
          error={inPast ? 'The start date cannot be in the past.' : undefined}
        >
          <Input type="date" min={minDate} value={startDate} onChange={(e) => onStartDateChange(e.target.value)} />
        </FormField>

        <div className="space-y-1.5">
          <FieldLabel>Training days *</FieldLabel>
          <div className="flex flex-wrap gap-1.5">
            {DAYS_OF_WEEK.map((day) => {
              const active = trainingDays.includes(day.value);
              return (
                <Button
                  key={day.value}
                  size="sm"
                  type="button"
                  variant={active ? 'primary' : 'secondary'}
                  aria-pressed={active}
                  onClick={() => onToggleDay(day.value)}
                >
                  {day.label}
                </Button>
              );
            })}
          </div>
        </div>
      </div>

      {preview && !inPast && (
        <div
          className={cn(
            'flex flex-wrap items-center gap-x-4 gap-y-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] px-3 py-2 text-xs text-[var(--color-text-secondary)] transition-opacity duration-[var(--duration-fast)]',
            stale && 'opacity-60',
          )}
        >
          <span className="flex items-center gap-1.5 font-semibold text-[var(--color-text-primary)]">
            <Icon name="calendar" size={13} />
            {preview.totalSessions} sessions · {formatDate(preview.firstSessionDate, DAY_WITH_DATE)} →{' '}
            {formatDate(preview.endDate, DAY_WITH_DATE)}
          </span>
          {preview.scheduleFullDates.length > 0 && (
            <span className="flex items-center gap-1.5 font-semibold text-[var(--color-danger)]">
              <Icon name="alert-triangle" size={13} />
              Schedule full on {preview.scheduleFullDates.map((date) => formatDate(date)).join(', ')}
            </span>
          )}
        </div>
      )}
    </Panel>
  );
}
