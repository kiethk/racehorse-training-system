'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  FilterChips,
  MetricCard,
  Notice,
  PageHeader,
  Panel,
  Pill,
  ProgressBar,
  ScreenLayout,
  LinkButton,
  SectionTitle,
  Tooltip,
} from '@/components/ui';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { HorseSpecialtyBadges } from '@/features/horses/shared/components/HorseSpecialtyBadges';
import { displayError, formatNumber } from '@/lib/display';
import { RATING_LEGEND, ratingDistribution } from '@/features/training/lib/rating';
import { cancelledLabel, planProgress } from '@/features/training/lib/planProgress';
import { PLAN_STATUS } from '@/features/training/lib/status';
import { StatusPill } from '@/features/training/components/StatusPill';
import { dashboardService } from '../services/dashboardService';
import type { DashboardHorse } from '../types';
import { HealthTrendCard } from './HealthTrendCard';
import { PerformanceTrendCard } from './PerformanceTrendCard';

type HorseFilter = 'ALL' | 'ACTIVE' | 'ALERT';

/**
 * The third line of the course cell: the bar and the held/planned count, counted the same way as on the Plan page.
 * Cancelled sessions are named in the tooltip to keep the row short.
 */
function CourseProgress({ horse }: { horse: DashboardHorse }) {
  const progress = planProgress(horse.completedSessions, horse.totalSessions, horse.cancelledSessions, horse.planStatus);
  if (progress.allCancelled) {
    return <p className="text-xs text-[var(--color-text-muted)]">All {progress.cancelled} sessions cancelled</p>;
  }
  if (progress.total === 0) return null;

  const count = <span className="shrink-0">{progress.completed}/{progress.total}</span>;
  return (
    <div className="flex items-center gap-2 text-xs text-[var(--color-text-secondary)]">
      <ProgressBar label={`${horse.horseName} course progress`} value={progress.percent} size="sm" tone="info" className="flex-1" />
      {progress.cancelled > 0 ? (
        <Tooltip content={cancelledLabel(progress.cancelled)}>
          <span tabIndex={0} className="shrink-0 rounded-[var(--radius-xs)] underline decoration-dotted outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">
            {progress.completed}/{progress.total}
          </span>
        </Tooltip>
      ) : count}
    </div>
  );
}

function columnsFor(horseBasePath: string): DataTableColumn<DashboardHorse>[] {
  return [
  {
    id: 'horse',
    header: 'Horse',
    sortValue: (horse) => horse.horseName,
    render: (horse) => (
      <div className="min-w-0">
        <Link
          href={`${horseBasePath}/${horse.horseId}`}
          className="font-semibold hover:text-[var(--color-primary)] focus-visible:rounded-sm focus-visible:outline-2"
        >
          {horse.horseName}
        </Link>
        <div className="text-xs text-[var(--color-text-muted)]">
          {[horse.stallCode, horse.breed].filter(Boolean).join(' · ') || '—'}
        </div>
      </div>
    ),
  },
  {
    id: 'specialty',
    header: 'Specialty',
    render: (horse) => <HorseSpecialtyBadges specialty={horse.specialty} stacked />,
  },
  {
    // Course, plan status and progress share one column so the table fits without scrolling sideways.
    id: 'course',
    header: 'Training course',
    className: 'min-w-32',
    sortValue: (horse) => horse.progressPercent,
    render: (horse) =>
      horse.planStatus === 'NONE' ? (
        <span className="text-[var(--color-text-muted)]">Not enrolled</span>
      ) : (
        // Always three lines (course, status, progress) so every row reads the same way.
        <div className="space-y-1">
          <div className="text-[var(--color-text-secondary)]">{horse.courseName}</div>
          <StatusPill status={PLAN_STATUS[horse.planStatus]} />
          <CourseProgress horse={horse} />
        </div>
      ),
  },
  {
    // The latest session rating and the 30-day average, each with its own label.
    id: 'rating',
    header: 'Performance',
    sortValue: (horse) => horse.avgPerformanceRating30d,
    render: (horse) => (
      <dl className="grid grid-cols-[auto_auto] items-center gap-x-2 gap-y-1 text-xs">
        <dt className="whitespace-nowrap text-[var(--color-text-muted)]">Latest</dt>
        <dd className="whitespace-nowrap font-semibold text-[var(--color-text-primary)]">
          {horse.latestPerformanceRating != null ? `${horse.latestPerformanceRating}/10` : '—'}
        </dd>
        <dt className="whitespace-nowrap text-[var(--color-text-muted)]">30-day avg</dt>
        <dd className="whitespace-nowrap font-semibold text-[var(--color-text-primary)]">
          {horse.avgPerformanceRating30d != null ? `${formatNumber(horse.avgPerformanceRating30d, 1)}/10` : '—'}
        </dd>
      </dl>
    ),
  },
  {
    id: 'alerts',
    header: 'Alerts',
    align: 'center',
    sortValue: (horse) => horse.alertsCount,
    render: (horse) =>
      horse.alertsCount > 0 ? (
        <Tooltip content={horse.alertTitles.join(' · ')}>
          <span tabIndex={0} className="inline-flex rounded-full outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]">
            <Pill tone="danger" size="sm" icon="alert-triangle">{horse.alertsCount}</Pill>
          </span>
        </Tooltip>
      ) : (
        <Pill tone="success" size="sm" icon="check">Clear</Pill>
      ),
  },
  {
    id: 'actions',
    header: <span className="sr-only">Actions</span>,
    align: 'right',
    render: (horse) => (
      <LinkButton href={`${horseBasePath}/${horse.horseId}`} size="sm" variant="secondary">
        Details
      </LinkButton>
    ),
  },
  ];
}

/**
 * The dashboard of a role that looks after horses: counts, a performance chart, an optional health chart, how the
 * team rates, and a table of the horses with their current plan. The backend scopes the data to the viewer (an owner
 * sees own horses, a trainer the horses of the areas, the manager every horse), so this screen never selects its
 * content by role: each actor page composes it with its own title, link target and charts.
 */
export function HorseDashboardView({
  title,
  horseBasePath,
  showHealth = false,
}: {
  title: string;
  /** Where a horse opens, without the id: for example "/owner/horses". */
  horseBasePath: string;
  /** Adds the health chart below the performance chart. */
  showHealth?: boolean;
}) {
  const columns = useMemo(() => columnsFor(horseBasePath), [horseBasePath]);
  const [horses, setHorses] = useState<DashboardHorse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<HorseFilter>('ALL');

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setHorses(await dashboardService.getHorses());
    } catch (cause) {
      setError(displayError(cause, 'Unable to load training dashboard data.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const inTraining = useMemo(() => horses.filter((h) => h.planStatus === 'ACTIVE'), [horses]);
  const withAlerts = useMemo(() => horses.filter((h) => h.alertsCount > 0), [horses]);

  const averageRating = useMemo(() => {
    const ratings = horses.map((h) => h.avgPerformanceRating30d).filter((r): r is number => r != null);
    return ratings.length > 0 ? ratings.reduce((sum, r) => sum + r, 0) / ratings.length : null;
  }, [horses]);

  const distribution = useMemo(
    () => ratingDistribution(horses.map((h) => h.avgPerformanceRating30d ?? h.latestPerformanceRating)),
    [horses],
  );

  const visibleHorses = filter === 'ACTIVE' ? inTraining : filter === 'ALERT' ? withAlerts : horses;

  return (
    <ScreenLayout variant="dashboard">
      <PageHeader title={title} />

      {error && <Notice tone="error">{error}</Notice>}

      <div className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-4">
        <MetricCard label="Total horses" value={horses.length} icon="horse" className="min-h-24" />
        <MetricCard label="In training" value={inTraining.length} icon="activity" tone="info" className="min-h-24" />
        <MetricCard
          label="Have risk alerts"
          value={withAlerts.length}
          icon="alert-triangle"
          tone={withAlerts.length > 0 ? 'danger' : 'success'}
          className="min-h-24"
        />
        <MetricCard
          label="Avg performance (30 days)"
          value={averageRating != null ? `${formatNumber(averageRating, 1)}/10` : '—'}
          icon="trending-up"
          tone="success"
          className="min-h-24"
        />
      </div>

      <PerformanceTrendCard />
      {showHealth && <HealthTrendCard />}

      <Panel padded className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <SectionTitle>Performance distribution</SectionTitle>
          <div className="flex flex-wrap items-center gap-2">
            <Pill tone="success" size="sm">{RATING_LEGEND.excellent}: {distribution.excellent}</Pill>
            <Pill tone="info" size="sm">{RATING_LEGEND.good}: {distribution.good}</Pill>
            <Pill tone="warning" size="sm">{RATING_LEGEND.needsImprovement}: {distribution.needsImprovement}</Pill>
            <Pill tone="neutral" size="sm">No data: {distribution.noData}</Pill>
          </div>
        </div>
        {horses.length > 0 && (
          <ProgressBar
            label="Performance distribution"
            max={horses.length}
            segments={[
              { value: distribution.excellent, tone: 'success', label: `Excellent: ${distribution.excellent}` },
              { value: distribution.good, tone: 'info', label: `On target: ${distribution.good}` },
              { value: distribution.needsImprovement, tone: 'warning', label: `Needs improvement: ${distribution.needsImprovement}` },
              { value: distribution.noData, tone: 'neutral', label: `No data: ${distribution.noData}` },
            ]}
          />
        )}
      </Panel>

      <div className="space-y-3">
        <FilterChips
          label="Horse filter"
          value={filter}
          onChange={setFilter}
          options={[
            { value: 'ALL', label: 'All', count: horses.length },
            { value: 'ACTIVE', label: 'In training', count: inTraining.length },
            { value: 'ALERT', label: 'With alerts', count: withAlerts.length },
          ]}
        />
        <DataTable
          ariaLabel="Assigned horses"
          rows={visibleHorses}
          columns={columns}
          getRowKey={(horse) => horse.horseId}
          loading={loading}
          pageSize={8}
          getSearchText={(horse) => `${horse.horseName} ${horse.courseName} ${horse.stallCode ?? ''}`}
          searchPlaceholder="Search horses…"
          emptyTitle={horses.length === 0 ? 'No horses yet' : 'No horses match the selected filters'}
        />
      </div>
    </ScreenLayout>
  );
}
