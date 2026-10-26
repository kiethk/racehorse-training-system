'use client';
import { hydrationStatusLabel } from '@/features/health/lib/hydrationStatus';

import { EmptyState, MetricCard, Panel } from '@/components/ui';
import type { HorseHealthMetric } from '@/features/horses/types';
import { formatDateTime, formatNumber } from '@/lib/display';

function value(value: number | null | undefined, fractionDigits = 0): string {
  return value == null ? '—' : formatNumber(value, fractionDigits);
}

/** Latest examination vitals plus the earlier measurements of the same horse. */
export function HorseVitalsPanel({ metrics }: { metrics: HorseHealthMetric[] }) {
  const [latest, ...previous] = metrics;

  if (!latest) {
    return (
      <Panel padded>
        <EmptyState
          icon="activity"
          title="No vitals recorded"
          description="No veterinary examination vitals have been logged for this horse yet."
        />
      </Panel>
    );
  }

  return (
    <Panel padded className="space-y-4">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Latest vitals</h3>
        <span className="text-xs text-[var(--color-text-muted)]">Recorded {formatDateTime(latest.recordedAt)}</span>
      </div>

      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <MetricCard label="Heart rate" value={value(latest.heartRate)} unit="bpm" icon="heart-pulse" />
        <MetricCard label="Temperature" value={value(latest.temperature, 1)} unit="°C" icon="activity" />
        <MetricCard label="Weight" value={value(latest.weight, 1)} unit="kg" icon="gauge" />
        <MetricCard label="Respiratory rate" value={value(latest.respiratoryRate)} unit="bpm" icon="trending-up" />
      </div>

      <dl className="grid grid-cols-2 gap-3 border-t border-[var(--color-border)] pt-3 text-sm sm:grid-cols-3">
        <div>
          <dt className="text-xs text-[var(--color-text-muted)]">Hydration</dt>
          <dd className="font-medium text-[var(--color-text-primary)]">{hydrationStatusLabel(latest.hydrationStatus) || '—'}</dd>
        </div>
        <div>
          <dt className="text-xs text-[var(--color-text-muted)]">Body condition score</dt>
          <dd className="font-medium text-[var(--color-text-primary)]">
            {latest.bodyConditionScore == null ? '—' : `${value(latest.bodyConditionScore, 1)}/9`}
          </dd>
        </div>
        <div>
          <dt className="text-xs text-[var(--color-text-muted)]">Health record</dt>
          <dd className="font-medium text-[var(--color-text-primary)]">
            {latest.healthRecordId ? `#${latest.healthRecordId}` : '—'}
          </dd>
        </div>
      </dl>

      {latest.notes && (
        <p className="border-t border-[var(--color-border)] pt-3 text-sm italic text-[var(--color-text-secondary)]">
          &ldquo;{latest.notes}&rdquo;
        </p>
      )}

      {previous.length > 0 && (
        <div className="space-y-1.5 border-t border-[var(--color-border)] pt-3">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-[var(--color-text-muted)]">
            Earlier measurements
          </h4>
          <ul className="space-y-1 text-xs text-[var(--color-text-secondary)]">
            {previous.slice(0, 5).map((metric) => (
              <li key={metric.id} className="flex flex-wrap items-center gap-x-3">
                <span className="font-medium text-[var(--color-text-primary)]">{formatDateTime(metric.recordedAt)}</span>
                <span>HR {value(metric.heartRate)} bpm</span>
                <span>Temp {value(metric.temperature, 1)} °C</span>
                <span>Weight {value(metric.weight, 1)} kg</span>
              </li>
            ))}
          </ul>
        </div>
      )}
    </Panel>
  );
}
