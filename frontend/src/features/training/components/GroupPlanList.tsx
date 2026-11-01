'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import { Button, EmptyState, LinkButton, ListSkeleton, PageHeader, Panel, ProgressBar, ScreenLayout, Select } from '@/components/ui';
import { useHorses } from '@/features/horses/hooks/useHorses';
import { displayError, formatDate } from '@/lib/display';
import { groupLabel } from '../lib/disbandMessage';
import { groupPlansByGroup, type PlanBlock } from '../lib/planBlocks';
import { planProgress } from '../lib/planProgress';
import { PLAN_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { GroupPlanSummary, TrainingGroup } from '../types';
import { StatusPill } from './StatusPill';

/** The Trainer's plans, one block per group (live groups first, Disbanded ones last and collapsed), with a horse filter. */
export function GroupPlanList() {
  const [groups, setGroups] = useState<TrainingGroup[] | null>(null);
  const [plans, setPlans] = useState<GroupPlanSummary[] | null>(null);
  const [horseId, setHorseId] = useState<number | ''>('');
  const [error, setError] = useState<string | null>(null);
  const { horses } = useHorses({ mine: true });

  async function load() {
    setError(null);
    try {
      const [loadedGroups, loadedPlans] = await Promise.all([
        trainingService.getGroups(),
        trainingService.getGroupPlans(horseId === '' ? undefined : { horseId }),
      ]);
      setGroups(loadedGroups);
      setPlans(loadedPlans);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the plans.'));
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [horseId]);

  const blocks = useMemo(() => {
    if (!groups || !plans) return null;
    const all = groupPlansByGroup(groups, plans);
    // With a horse filter only the groups that have a matching plan are of interest.
    return horseId === '' ? all : all.filter((block) => block.plans.length > 0);
  }, [groups, plans, horseId]);
  const live = blocks?.filter((block) => !block.disbanded) ?? [];
  const disbanded = blocks?.filter((block) => block.disbanded) ?? [];

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Plans"
        actions={<LinkButton href="/trainer/plans/new" variant="primary" icon="plus">Assign a course</LinkButton>}
      />
      <div className="max-w-xs">
        <Select
          aria-label="Filter by horse"
          value={horseId}
          onChange={(event) => setHorseId(event.target.value ? Number(event.target.value) : '')}
        >
          <option value="">All horses</option>
          {horses.map((horse) => (
            <option key={horse.id} value={horse.id}>{horse.name}</option>
          ))}
        </Select>
      </div>
      {error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load the plans"
            description={error}
            action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
          />
        </Panel>
      ) : !blocks ? (
        <Panel padded><ListSkeleton rows={4} /></Panel>
      ) : blocks.length === 0 ? (
        <Panel padded><EmptyState icon="layers" title={horseId === '' ? 'No plans yet' : 'No plans for this horse'} /></Panel>
      ) : (
        <div className="space-y-6">
          {live.map((block) => (
            <GroupBlock key={block.groupId} block={block} />
          ))}
          {disbanded.length > 0 && <DisbandedBlocks blocks={disbanded} />}
        </div>
      )}
    </ScreenLayout>
  );
}

function DisbandedBlocks({ blocks }: { blocks: PlanBlock[] }) {
  const [open, setOpen] = useState(false);
  return (
    <section className="space-y-3">
      <Button
        variant="tertiary"
        size="sm"
        icon={open ? 'chevron-down' : 'chevron-right'}
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        Disbanded ({blocks.length})
      </Button>
      {open && (
        <div className="space-y-6">
          {blocks.map((block) => (
            <GroupBlock key={block.groupId ?? 'none'} block={block} />
          ))}
        </div>
      )}
    </section>
  );
}

function GroupBlock({ block }: { block: PlanBlock }) {
  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-semibold text-[var(--color-text-primary)]">{groupLabel(block.name, block.disbanded)}</h2>
          <p className="text-xs text-[var(--color-text-secondary)]">
            {block.disbanded ? '' : `${block.horseCount} ${block.horseCount === 1 ? 'horse' : 'horses'} · `}
            {block.plans.length} {block.plans.length === 1 ? 'plan' : 'plans'}
          </p>
        </div>
        {!block.disbanded && block.plans.length === 0 && block.groupId !== null && (
          <LinkButton href={`/trainer/plans/new?groupId=${block.groupId}`} size="sm" icon="plus">Assign a course</LinkButton>
        )}
      </div>
      {block.plans.length > 0 && (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {block.plans.map((plan) => (
            <PlanCard key={plan.id} plan={plan} />
          ))}
        </div>
      )}
    </section>
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
