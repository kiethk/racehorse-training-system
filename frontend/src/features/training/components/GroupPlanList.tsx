'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Button, EmptyState, LinkButton, ListSkeleton, PageHeader, Panel, ProgressBar, ScreenLayout } from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { planProgress } from '../lib/planProgress';
import { PLAN_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { GroupPlanSummary } from '../types';
import { StatusPill } from './StatusPill';
import { groupLabel } from '../lib/disbandMessage';

/** The Trainer's group plans, newest first, each with its progress. */
export function GroupPlanList() {
  const [plans, setPlans] = useState<GroupPlanSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function load() {
    setError(null);
    try {
      setPlans(await trainingService.getGroupPlans());
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the plans.'));
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, []);

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Plans"
        actions={<LinkButton href="/trainer/plans/new" variant="primary" icon="plus">Assign a course</LinkButton>}
      />
      {error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load the plans"
            description={error}
            action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
          />
        </Panel>
      ) : !plans ? (
        <Panel padded><ListSkeleton rows={4} /></Panel>
      ) : plans.length === 0 ? (
        <Panel padded><EmptyState icon="layers" title="No plans yet" /></Panel>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {plans.map((plan) => (
            <PlanCard key={plan.id} plan={plan} />
          ))}
        </div>
      )}
    </ScreenLayout>
  );
}

function PlanCard({ plan }: { plan: GroupPlanSummary }) {
  const progress = planProgress(plan.sessionsHeld, plan.sessionsTotal, plan.sessionsCancelled, plan.status);

  return (
    <Link href={`/trainer/plans/${plan.id}`} className="block">
      <Panel padded interactive className="flex h-full flex-col gap-3">
        <div className="flex items-start justify-between gap-2">
          <div className="min-w-0">
            <h3 className="truncate text-base font-semibold text-[var(--color-text-primary)]">{plan.courseName}</h3>
            <p className="truncate text-sm text-[var(--color-text-secondary)]">{groupLabel(plan.groupName, plan.groupDisbanded)}</p>
          </div>
          <StatusPill status={PLAN_STATUS[plan.status]} />
        </div>
        <p className="text-xs text-[var(--color-text-secondary)]">
          {formatDate(plan.startDate)} – {formatDate(plan.endDate)}
        </p>
        <div className="mt-auto space-y-1">
          <ProgressBar label="Plan progress" value={progress.percent} size="sm" />
          <p className="text-xs text-[var(--color-text-muted)]">{progress.completed}/{progress.total} sessions</p>
        </div>
      </Panel>
    </Link>
  );
}
