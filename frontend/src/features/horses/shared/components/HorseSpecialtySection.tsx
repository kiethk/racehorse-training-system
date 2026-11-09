import { Icon, ProgressBar, Tooltip } from '@/components/ui';
import type { HorseSpecialty } from '@/features/horses/types';
import { cn } from '@/lib/cn';
import { formatEnumLabel, formatNumber } from '@/lib/display';
import { HorseInfoSection } from './HorseInfoSection';

/** Read-only evidence from the backend; no local specialty thresholds or decisions. */
export function HorseSpecialtySection({ specialty }: { specialty: HorseSpecialty | null }) {
  const hasWorkouts = specialty?.distanceStats.some((stat) => stat.workoutCount > 0);

  return (
    <HorseInfoSection
      title="Racing specialty"
      icon="flag"
      aside={(
        <Tooltip content="Workouts in the last 90 days, recovery sessions excluded. The tick marks the average a distance needs, with at least the minimum rated workouts, to earn a badge. Faded bars do not have enough workouts yet.">
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
          {specialty.distanceStats.filter((stat) => stat.workoutCount > 0).map((stat) => {
            const judged = stat.workoutCount >= specialty.minWorkoutsPerGroup;
            return (
              <div key={stat.band} className="space-y-2">
                <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
                  <span className="font-medium text-[var(--color-text-primary)]">{formatEnumLabel(stat.band)}</span>
                  <span className="text-[var(--color-text-secondary)]">
                    {judged
                      ? `${formatNumber(stat.workoutCount)} rated`
                      : `${formatNumber(stat.workoutCount)}/${formatNumber(specialty.minWorkoutsPerGroup)} rated`}
                    {' · '}{formatNumber(stat.averageRating, 1)}/10 average
                  </span>
                </div>
                <div className={cn(!judged && 'opacity-40')}>
                  <ProgressBar
                    label={`${formatEnumLabel(stat.band)} average rating out of 10`}
                    value={stat.averageRating}
                    max={10}
                    marker={specialty.minAverageRating}
                    markerLabel={`Passing average: ${formatNumber(specialty.minAverageRating, 1)}/10`}
                    size="sm"
                  />
                </div>
              </div>
            );
          })}
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
