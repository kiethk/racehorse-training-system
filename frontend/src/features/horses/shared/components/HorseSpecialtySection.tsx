import { Icon, ProgressBar, Tooltip } from '@/components/ui';
import type { HorseSpecialty } from '@/features/horses/types';
import { formatEnumLabel, formatNumber } from '@/lib/display';
import { HorseInfoSection } from './HorseInfoSection';

/** Read-only evidence from the backend; no local specialty thresholds or decisions. */
export function HorseSpecialtySection({ specialty }: { specialty: HorseSpecialty | null }) {
  const hasWorkouts = specialty?.distanceStats.some((stat) => stat.workoutCount > 0);

  return (
    <HorseInfoSection
      title="Racing profile"
      icon="flag"
      aside={(
        <Tooltip content="Workouts in the last 90 days, recovery sessions excluded">
          <span
            tabIndex={0}
            aria-label="Which workouts are counted"
            className="inline-flex rounded-full text-[var(--color-text-muted)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
          >
            <Icon name="info" size={16} />
          </span>
        </Tooltip>
      )}
    >
      {hasWorkouts && specialty ? (
        <div className="space-y-4">
          {specialty.distanceStats.filter((stat) => stat.workoutCount > 0).map((stat) => (
            <div key={stat.band} className="space-y-2">
              <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                <span className="font-medium text-[var(--color-text-primary)]">{formatEnumLabel(stat.band)}</span>
                <span className="text-[var(--color-text-secondary)]">
                  {formatNumber(stat.workoutCount)} rated · {formatNumber(stat.averageRating, 1)}/10 average
                </span>
              </div>
              <ProgressBar label={`${formatEnumLabel(stat.band)} rated workouts toward minimum evidence`} value={stat.workoutCount} max={specialty.minWorkoutsPerGroup} size="sm" />
            </div>
          ))}
        </div>
      ) : (
        <div className="flex items-start gap-3 rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] p-4">
          <Icon name="trending-up" size={22} className="mt-0.5 shrink-0 text-[var(--color-primary)]" />
          <p className="font-medium text-[var(--color-text-primary)]">{specialty ? 'No rated workouts yet' : 'Specialty unavailable'}</p>
        </div>
      )}
    </HorseInfoSection>
  );
}
