'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'next/navigation';
import {
  Button,
  EmptyState,
  LinkButton,
  ListSkeleton,
  PageHeader,
  Panel,
  ProgressBar,
  ScreenLayout,
} from '@/components/ui';
import { HorseFilterBar } from '@/features/horses/shared/components/HorseFilterBar';
import { displayError, formatDate } from '@/lib/display';
import { cancelledLabel, planProgress } from '../lib/planProgress';
import { planOrder } from '../lib/listOrder';
import { PLAN_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { PlanStatus, PlanSummaryResponse } from '../types';
import { StatusPill } from './StatusPill';

const STATUS_OPTIONS = (['UPCOMING', 'ACTIVE', 'COMPLETED', 'CANCELLED'] as const).map((status) => ({
  value: status,
  label: PLAN_STATUS[status].label,
}));

function PlanRow({ plan }: { plan: PlanSummaryResponse }) {
  const progress = planProgress(plan.completedSessions, plan.totalSessions, plan.cancelledSessions, plan.status);

  return (
    <Panel padded>
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div className="min-w-0 space-y-0.5">
          <div className="flex flex-wrap items-center gap-2">
            <strong className="text-base text-[var(--color-text-primary)]">{plan.horseName}</strong>
            <StatusPill status={PLAN_STATUS[plan.status]} />
          </div>
          <p className="text-xs text-[var(--color-text-secondary)]">{plan.courseName}</p>
          <p className="text-xs text-[var(--color-text-muted)]">
            {formatDate(plan.startDate)} → {formatDate(plan.endDate)}
          </p>
        </div>

        <div className="flex items-center gap-4">
          <div className="w-44 space-y-1">
            {progress.allCancelled ? (
              <p className="text-xs text-[var(--color-text-muted)]">All {progress.cancelled} sessions cancelled</p>
            ) : (
              <>
                <div className="flex justify-between text-xs text-[var(--color-text-secondary)]">
                  <span>{progress.completed}/{progress.total} sessions</span>
                  <span>{progress.percent}%</span>
                </div>
                <ProgressBar label={`${plan.horseName} plan progress`} value={progress.percent} size="sm" />
                {progress.cancelled > 0 && (
                  <p className="text-xs text-[var(--color-text-muted)]">{cancelledLabel(progress.cancelled)}</p>
                )}
              </>
            )}
          </div>
          <LinkButton href={`/trainer/plans/${plan.planId}`} size="sm">Details</LinkButton>
        </div>
      </div>
    </Panel>
  );
}

export function PlanList() {
  // The horse page links here with ?search=<horse name>, so its plans are already filtered.
  const initialSearch = useSearchParams().get('search') ?? '';
  const [plans, setPlans] = useState<PlanSummaryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [draftSearch, setDraftSearch] = useState(initialSearch);
  const [draftStatus, setDraftStatus] = useState<PlanStatus | ''>('');
  const [appliedSearch, setAppliedSearch] = useState(initialSearch.trim());
  const [appliedStatus, setAppliedStatus] = useState<PlanStatus | ''>('');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setPlans(await trainingService.getPlans());
    } catch (cause) {
      setError(displayError(cause, 'Unable to load training plans.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const visible = useMemo(() => {
    const query = appliedSearch.toLowerCase();
    return plans
      .filter((plan) =>
        (!appliedStatus || plan.status === appliedStatus)
        && (!query || plan.horseName.toLowerCase().includes(query)))
      .sort(planOrder);
  }, [plans, appliedSearch, appliedStatus]);

  const applyFilters = () => {
    setAppliedSearch(draftSearch.trim());
    setAppliedStatus(draftStatus);
  };

  const clearFilters = () => {
    setDraftSearch('');
    setDraftStatus('');
    setAppliedSearch('');
    setAppliedStatus('');
  };

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Training plans"
        actions={<LinkButton href="/trainer/plans/new" variant="primary" size="sm" icon="plus">Enroll horses</LinkButton>}
      />

      <HorseFilterBar
        search={draftSearch}
        onSearchChange={setDraftSearch}
        searchPlaceholder="Search by horse name…"
        status={draftStatus}
        onStatusChange={setDraftStatus}
        statusOptions={STATUS_OPTIONS}
        onApply={applyFilters}
        onClear={clearFilters}
      />

      {loading ? (
        <Panel padded><ListSkeleton rows={6} /></Panel>
      ) : error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load training plans"
            description={error}
            action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
          />
        </Panel>
      ) : visible.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="clipboard"
            title={plans.length === 0 ? 'No training plans yet' : 'No plans match these filters'}
            description={plans.length === 0 ? 'Enroll a horse in a training course to get started.' : undefined}
          />
        </Panel>
      ) : (
        <div className="space-y-2">
          {visible.map((plan) => <PlanRow key={plan.planId} plan={plan} />)}
        </div>
      )}
    </ScreenLayout>
  );
}
