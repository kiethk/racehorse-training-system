'use client';

import Link from 'next/link';
import { useEffect, useMemo, useState } from 'react';
import {
  Button,
  EmptyState,
  FilterBar,
  FormField,
  Input,
  LinkButton,
  ListSkeleton,
  PageHeader,
  Panel,
  Pill,
  ProgressBar,
  ScreenLayout,
  Select,
} from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { plural } from '../lib/groupLifecycle';
import { groupPlansByGroup, type PlanBlock } from '../lib/planBlocks';
import { planProgress } from '../lib/planProgress';
import { PLAN_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { Course, GroupPlanSummary, PlanStatus, TrainingGroup } from '../types';
import { StatusPill } from './StatusPill';

/**
 * The Trainer's plans, grouped by training group (active groups first, disbanded groups following),
 * with a comprehensive filter bar (dates, group, status, course).
 */
export function GroupPlanList() {
  const [groups, setGroups] = useState<TrainingGroup[] | null>(null);
  const [plans, setPlans] = useState<GroupPlanSummary[] | null>(null);
  const [courses, setCourses] = useState<Course[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Filters
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [groupId, setGroupId] = useState<number | ''>('');
  const [courseId, setCourseId] = useState<number | ''>('');
  const [status, setStatus] = useState<PlanStatus | ''>('');

  async function load() {
    setError(null);
    try {
      const [loadedGroups, loadedPlans, loadedCourses] = await Promise.all([
        trainingService.getGroups(),
        trainingService.getGroupPlans(),
        trainingService.getCourses(),
      ]);
      setGroups(loadedGroups);
      setPlans(loadedPlans);
      setCourses(loadedCourses);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the plans.'));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, []);

  const hasFilter = Boolean(fromDate || toDate || groupId !== '' || courseId !== '' || status);

  function resetFilters() {
    setFromDate('');
    setToDate('');
    setGroupId('');
    setCourseId('');
    setStatus('');
  }

  const filteredPlans = useMemo(() => {
    if (!plans) return null;
    return plans.filter((plan) => {
      if (groupId !== '' && plan.groupId !== Number(groupId)) return false;
      if (courseId !== '' && plan.courseId !== Number(courseId)) return false;
      if (status && plan.status !== status) return false;
      if (fromDate && plan.endDate < fromDate) return false;
      if (toDate && plan.startDate > toDate) return false;
      return true;
    });
  }, [plans, groupId, courseId, status, fromDate, toDate]);

  const blocks = useMemo(() => {
    if (!groups || !filteredPlans) return null;
    const all = groupPlansByGroup(groups, filteredPlans);
    // If a filter is active, only show groups with matching plans
    return hasFilter ? all.filter((block) => block.plans.length > 0) : all;
  }, [groups, filteredPlans, hasFilter]);

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Plans"
        actions={<LinkButton href="/trainer/plans/new" variant="primary" icon="plus">New Plan</LinkButton>}
      />

      {/* Comprehensive FilterBar stretching full width */}
      <FilterBar
        onSubmit={(e) => e.preventDefault()}
        layout="flex"
        className="w-full flex-wrap gap-3 items-end"
      >
        <FormField label="From date" className="flex-1 min-w-[150px]">
          <Input
            type="date"
            lang="en"
            value={fromDate}
            onChange={(e) => setFromDate(e.target.value)}
            className="w-full"
          />
        </FormField>
        <FormField label="To date" className="flex-1 min-w-[150px]">
          <Input
            type="date"
            lang="en"
            value={toDate}
            onChange={(e) => setToDate(e.target.value)}
            className="w-full"
          />
        </FormField>
        <FormField label="Group" className="flex-1 min-w-[170px]">
          <Select
            value={groupId}
            onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : '')}
            className="w-full"
          >
            <option value="">All groups</option>
            {groups?.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Course" className="flex-1 min-w-[170px]">
          <Select
            value={courseId}
            onChange={(e) => setCourseId(e.target.value ? Number(e.target.value) : '')}
            className="w-full"
          >
            <option value="">All courses</option>
            {courses.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
        </FormField>
        <FormField label="Status" className="flex-1 min-w-[150px]">
          <Select
            value={status}
            onChange={(e) => setStatus(e.target.value as PlanStatus | '')}
            className="w-full"
          >
            <option value="">All statuses</option>
            <option value="UPCOMING">Upcoming</option>
            <option value="ACTIVE">Active</option>
            <option value="COMPLETED">Completed</option>
            <option value="CANCELLED">Cancelled</option>
          </Select>
        </FormField>
        {hasFilter && (
          <div className="flex items-end pb-0.5 shrink-0">
            <Button size="sm" variant="secondary" onClick={resetFilters}>
              Clear filters
            </Button>
          </div>
        )}
      </FilterBar>

      {error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load the plans"
            description={error}
            action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
          />
        </Panel>
      ) : loading || !blocks ? (
        <Panel padded><ListSkeleton rows={4} /></Panel>
      ) : blocks.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="layers"
            title={hasFilter ? 'No plans match the filter' : 'No plans yet'}
            action={hasFilter ? <Button size="sm" onClick={resetFilters}>Reset filters</Button> : undefined}
          />
        </Panel>
      ) : (
        <div className="space-y-6">
          {blocks.map((block) => (
            <GroupBlock key={block.groupId ?? `disbanded-${block.name}`} block={block} />
          ))}
        </div>
      )}
    </ScreenLayout>
  );
}

const PLANS_PER_GROUP_INITIAL = 3;

function GroupBlock({ block }: { block: PlanBlock }) {
  const [expanded, setExpanded] = useState(false);
  const total = block.plans.length;
  const hasMore = total > PLANS_PER_GROUP_INITIAL;
  const visiblePlans = expanded ? block.plans : block.plans.slice(0, PLANS_PER_GROUP_INITIAL);
  const remaining = total - PLANS_PER_GROUP_INITIAL;

  return (
    <section className="space-y-3">
      <div className="flex items-center justify-between gap-2 border-b border-[var(--color-border)] pb-2">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-base font-semibold text-[var(--color-text-primary)]">{block.name}</h2>
          {block.disbanded ? (
            <Pill tone="neutral" size="sm">Disbanded</Pill>
          ) : (
            <span className="text-xs text-[var(--color-text-secondary)]">
              ({plural(block.horseCount, 'horse')})
            </span>
          )}
          <span className="text-xs text-[var(--color-text-muted)]">
            · {plural(total, 'plan')}
          </span>
        </div>

        {hasMore && (
          <Button
            variant="tertiary"
            size="sm"
            icon={expanded ? 'chevron-up' : 'chevron-down'}
            onClick={() => setExpanded((prev) => !prev)}
            className="shrink-0 text-xs font-medium text-[var(--color-primary)] hover:text-[var(--color-primary-hover)] h-auto py-1 px-2.5"
          >
            {expanded ? 'Show less' : `Show ${remaining} more ${remaining === 1 ? 'plan' : 'plans'}`}
          </Button>
        )}
      </div>

      {total === 0 ? (
        <p className="py-2 text-xs text-[var(--color-text-muted)] italic">No plans yet for this group.</p>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {visiblePlans.map((plan) => (
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
