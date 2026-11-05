import { formatDate, formatNumber } from '@/lib/display';
import { cn } from '@/lib/cn';
import type { HorseFitnessTrendItem, TrainingRules } from '../../types';

export interface WorkoutHover {
  x: number;
  y: number;
  item: HorseFitnessTrendItem;
}

const floating =
  'pointer-events-none fixed z-[var(--z-dropdown)] -translate-x-1/2 -translate-y-full rounded-[var(--radius-md)] ' +
  'border bg-[var(--color-surface)] p-2.5 shadow-[var(--shadow-popover)]';

/** Details of the session under the pointer. Over-limit heart rates are highlighted with the backend limits. */
export function WorkoutTooltip({ hover, rules }: { hover: WorkoutHover; rules: TrainingRules | null }) {
  const { item } = hover;
  const maxOver = rules != null && item.maxHeartRate != null && item.maxHeartRate > rules.maxHeartRateLimit;
  const recoveryOver = rules != null && item.recoveryHeartRate != null && item.recoveryHeartRate > rules.recoveryHeartRateLimit;

  return (
    <div className={cn(floating, 'border-[var(--color-border)]')} style={{ left: hover.x, top: hover.y - 10 }}>
      <p className="text-xs font-semibold text-[var(--color-text-primary)]">
        {formatDate(item.date)} · {item.subjectName}
      </p>
      <div className="mt-1 space-y-0.5 text-xs text-[var(--color-text-secondary)]">
        <p>
          Performance:{' '}
          <span className="font-semibold text-[var(--color-success)]">{item.performanceRating ?? 'N/A'}/10</span>
        </p>
        <p>
          Heart rate: avg {item.averageHeartRate ?? '—'} | max{' '}
          <strong className={maxOver ? 'text-[var(--color-danger)]' : undefined}>{item.maxHeartRate ?? '—'}</strong>
          {' '}| recovery{' '}
          <strong className={recoveryOver ? 'text-[var(--color-warning)]' : undefined}>{item.recoveryHeartRate ?? '—'}</strong> bpm
        </p>
        <p>Speed: avg {item.averageSpeedKmh ?? '—'} km/h | max {item.topSpeedKmh ?? '—'} km/h</p>
        {item.distanceMeters != null && <p>Distance: {formatNumber(item.distanceMeters)} m</p>}
      </div>
    </div>
  );
}
