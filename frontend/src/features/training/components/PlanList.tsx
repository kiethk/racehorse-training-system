'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { PlanSummaryResponse } from '../types';

type StatusFilter = 'ALL' | 'UPCOMING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

const STATUS_LABEL: Record<Exclude<StatusFilter, 'ALL'>, string> = {
  UPCOMING: 'Upcoming',
  ACTIVE: 'Training',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

const STATUS_TONE: Record<Exclude<StatusFilter, 'ALL'>, 'info' | 'success' | 'neutral'> = {
  UPCOMING: 'info',
  ACTIVE: 'success',
  COMPLETED: 'success',
  CANCELLED: 'neutral',
};

export function PlanList() {
  const [plans, setPlans] = useState<PlanSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<StatusFilter>('ALL');

  const load = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      setPlans(await trainingApi.getPlans());
    } catch (err) {
      console.error('Failed to load training plans:', err);
      setError('Unable to load training plans.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const filtered = useMemo(
    () => (filter === 'ALL' ? plans : plans.filter((p) => p.status === filter)),
    [plans, filter],
  );

  if (loading) return <ListSkeleton rows={6} />;

  if (error) {
    return (
      <Panel padded>
      <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
      </Panel>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-semibold tracking-tight text-[var(--color-text-primary)]">
          Training plans
          </h1>
          <p className="text-xs text-[var(--color-text-secondary)]">
          Track each horse’s training progress.
          </p>
        </div>
        <Link href="/trainer/plans/new">
        <Button variant="primary" size="sm" icon="plus">Enroll horses</Button>
        </Link>
      </div>

      <FilterChips
        label="Plan status"
        value={filter}
        onChange={setFilter}
        options={(['ALL', 'UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED'] as StatusFilter[]).map((s) => ({
          value: s,
          label: s === 'ALL' ? 'All' : STATUS_LABEL[s],
          count: s === 'ALL' ? plans.length : undefined,
        }))}
      />

      {filtered.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title="No training plans yet"
            description="Enroll a horse in a training course to get started."
          />
        </Panel>
      ) : (
        <div className="space-y-2">
          {filtered.map((p) => {
            // Cancelled sessions are removed from the denominator — a 12-session plan with 1 cancelled reaches
            // 100% at 11 sessions instead of hanging at 91% forever.
            const denominator = Math.max(p.totalSessions - p.cancelledSessions, 1);
            const percent = Math.min(
              Math.round((p.completedSessions / denominator) * 100),
              100,
            );

            return (
              <Panel key={p.planId} padded>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <strong className="text-base text-[var(--color-text-primary)]">
                        {p.horseName}
                      </strong>
                      <Pill tone={STATUS_TONE[p.status]} size="sm">
                        {STATUS_LABEL[p.status]}
                      </Pill>
                    </div>
                    <div className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
                      {p.courseName}
                    </div>
                    <div className="mt-0.5 text-xs text-[var(--color-text-muted)]">
                      {p.startDate} → {p.endDate}
                    </div>
                  </div>

                  <div className="flex items-center gap-4">
                    <div className="w-40">
                      <div className="flex justify-between text-xs text-[var(--color-text-secondary)]">
                        <span>
          {p.completedSessions}/{denominator} sessions
                        </span>
                        <span>{percent}%</span>
                      </div>
                      <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-surface-muted)]">
                        <div
                          className="h-full rounded-full bg-[var(--color-primary)]"
                          style={{ width: `${percent}%` }}
                        />
                      </div>
                      {p.cancelledSessions > 0 && (
                        <div className="mt-0.5 text-xs text-[var(--color-text-muted)]">
          {p.cancelledSessions} sessions cancelled
                        </div>
                      )}
                    </div>

                    <Link href={`/trainer/plans/${p.planId}`}>
          <Button variant="secondary" size="sm">Details</Button>
                    </Link>
                  </div>
                </div>
              </Panel>
            );
          })}
        </div>
      )}
    </div>
  );
}
