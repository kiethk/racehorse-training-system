'use client';

import Link from 'next/link';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  FilterChips,
  LinkButton,
  MetricCard,
  Notice,
  PageHeader,
  Pill,
  ProgressBar,
  ScreenLayout,
  Skeleton,
} from '@/components/ui';
import { DataTable, type DataTableColumn } from '@/components/ui/DataTable';
import { HorseSpecialtyBadges } from '@/features/horses/shared/components/HorseSpecialtyBadges';
import { displayError, formatNumber } from '@/lib/display';
import { PLAN_STATUS } from '@/features/training/lib/status';
import { RatingPill } from '@/features/training/components/RatingPill';
import { StatusPill } from '@/features/training/components/StatusPill';
import { dashboardService } from '../../services/dashboardService';
import type { DashboardHorse, ManagerDashboardSummary } from '../../types';
import { HealthTrendCard } from '../HealthTrendCard';
import { PerformanceLineTrendCard } from './PerformanceLineTrendCard';
import { BarnOccupancyWidget } from './BarnOccupancyWidget';
import { FleetDistributionWidget } from './FleetDistributionWidget';
import { ManagerActionCenter } from './ManagerActionCenter';

type HorseFilter = 'ALL' | 'ACTIVE' | 'ALERT';

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
            {[horse.stallCode ? `Stall ${horse.stallCode}` : null, horse.breed].filter(Boolean).join(' · ') || '—'}
          </div>
        </div>
      ),
    },
    {
      id: 'specialty',
      header: 'Specialty',
      render: (horse) => <HorseSpecialtyBadges specialty={horse.specialty} />,
    },
    {
      id: 'course',
      header: 'Training course',
      render: (horse) =>
        horse.planStatus === 'NONE' ? (
          <span className="text-[var(--color-text-muted)]">Not enrolled</span>
        ) : (
          <span className="text-[var(--color-text-secondary)]">{horse.courseName}</span>
        ),
    },
    {
      id: 'status',
      header: 'Status',
      sortValue: (horse) => PLAN_STATUS[horse.planStatus].label,
      render: (horse) => <StatusPill status={PLAN_STATUS[horse.planStatus]} />,
    },
    {
      id: 'progress',
      header: 'Course progress',
      className: 'min-w-36',
      sortValue: (horse) => horse.progressPercent,
      render: (horse) =>
        horse.totalSessions > 0 ? (
          <div className="space-y-1">
            <div className="flex items-center justify-between text-xs text-[var(--color-text-secondary)]">
              <span>{horse.completedSessions}/{horse.totalSessions} sessions</span>
              <span className="font-semibold">{formatNumber(horse.progressPercent)}%</span>
            </div>
            <ProgressBar label={`${horse.horseName} progress`} value={horse.progressPercent} size="sm" tone="info" />
          </div>
        ) : (
          <span className="text-[var(--color-text-muted)]">—</span>
        ),
    },
    {
      id: 'latest',
      header: 'Latest rating',
      align: 'center',
      sortValue: (horse) => horse.latestPerformanceRating,
      render: (horse) => <RatingPill rating={horse.latestPerformanceRating} />,
    },
    {
      id: 'average',
      header: '30-day average',
      align: 'center',
      sortValue: (horse) => horse.avgPerformanceRating30d,
      render: (horse) =>
        horse.avgPerformanceRating30d != null ? (
          <span className="font-semibold">{formatNumber(horse.avgPerformanceRating30d, 1)}</span>
        ) : (
          <span className="text-[var(--color-text-muted)]">—</span>
        ),
    },
    {
      id: 'alerts',
      header: 'Risk alerts',
      align: 'center',
      sortValue: (horse) => horse.alertsCount,
      render: (horse) =>
        horse.alertsCount > 0 ? (
          <Pill tone="danger" size="sm" icon="alert-triangle">
            {horse.alertsCount}
          </Pill>
        ) : (
          <Pill tone="success" size="sm" icon="check">
            Clear
          </Pill>
        ),
    },
    {
      id: 'actions',
      header: <span className="sr-only">Actions</span>,
      align: 'right',
      render: (horse) => (
        <LinkButton href={`${horseBasePath}/${horse.horseId}`} size="sm" icon="trending-up" variant="tertiary">
          Details
        </LinkButton>
      ),
    },
  ];
}

export function ManagerDashboardView() {
  const [summary, setSummary] = useState<ManagerDashboardSummary | null>(null);
  const [horses, setHorses] = useState<DashboardHorse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<HorseFilter>('ALL');

  const horseBasePath = '/manager/horses';
  const columns = useMemo(() => columnsFor(horseBasePath), [horseBasePath]);

  const loadData = useCallback(async (isManualRefresh = false) => {
    if (isManualRefresh) setRefreshing(true);
    else setLoading(true);
    setError(null);

    try {
      const [summaryData, horsesData] = await Promise.all([
        dashboardService.getManagerSummary(),
        dashboardService.getHorses(),
      ]);
      setSummary(summaryData);
      setHorses(horsesData);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load executive operations dashboard.'));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const inTraining = useMemo(() => horses.filter((h) => h.planStatus === 'ACTIVE'), [horses]);
  const withAlerts = useMemo(() => horses.filter((h) => h.alertsCount > 0), [horses]);

  const visibleHorses = filter === 'ACTIVE' ? inTraining : filter === 'ALERT' ? withAlerts : horses;

  return (
    <ScreenLayout variant="dashboard">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <PageHeader
          title="Executive Operations Hub"
          description="Real-time facility management, admission pipeline, and equine training strategy."
        />
        <div className="shrink-0 self-start sm:self-center">
          <Button
            size="sm"
            variant="secondary"
            icon="refresh"
            loading={refreshing}
            onClick={() => loadData(true)}
          >
            Refresh data
          </Button>
        </div>
      </div>

      {error && <Notice tone="error">{error}</Notice>}

      {/* Top Executive KPI Cards */}
      {loading && !summary ? (
        <div className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-4">
          <Skeleton className="h-24 w-full rounded-md" />
          <Skeleton className="h-24 w-full rounded-md" />
          <Skeleton className="h-24 w-full rounded-md" />
          <Skeleton className="h-24 w-full rounded-md" />
        </div>
      ) : summary ? (
        <div className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            label="Stall Occupancy"
            value={`${formatNumber(summary.stallSummary.occupancyRate, 1)}%`}
            icon="grid"
            tone={summary.stallSummary.occupancyRate >= 90 ? 'danger' : 'success'}
            hint={`${summary.stallSummary.occupiedStalls}/${summary.stallSummary.totalStalls} stalls used (${summary.stallSummary.availableStalls} available)`}
            className="min-h-24"
          />

          <MetricCard
            label="Total Fleet"
            value={summary.fleetSummary.totalHorses}
            icon="horse"
            tone="info"
            hint={`${summary.fleetSummary.inTraining} in training · ${summary.fleetSummary.activeEligible} eligible`}
            className="min-h-24"
          />

          <MetricCard
            label="Admission Pipeline"
            value={summary.admissionSummary.totalPending}
            icon="file-text"
            tone={summary.admissionSummary.managerReviewCount > 0 ? 'warning' : 'default'}
            hint={`${summary.admissionSummary.managerReviewCount} pending manager review · ${summary.admissionSummary.waitingArrivalCount} waiting arrival`}
            className="min-h-24"
          />

          <MetricCard
            label="Urgent Risks"
            value={summary.incidentSummary.criticalUrgentIncidents}
            icon="alert-triangle"
            tone={summary.incidentSummary.criticalUrgentIncidents > 0 ? 'danger' : 'success'}
            hint={
              summary.incidentSummary.criticalUrgentIncidents > 0
                ? `${summary.incidentSummary.criticalUrgentIncidents} High/Critical incident(s) require action`
                : 'No critical incidents open'
            }
            className="min-h-24"
          />
        </div>
      ) : null}

      {/* Urgent Action Center */}
      {summary && (
        <ManagerActionCenter
          pendingAdmissions={summary.pendingAdmissions}
          urgentIncidents={summary.urgentIncidents}
        />
      )}

      {/* Two-Column Grid: Stables & Fleet Readiness */}
      {summary && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <BarnOccupancyWidget
            summary={summary.stallSummary}
            barns={summary.barnOccupancies}
          />
          <FleetDistributionWidget
            summary={summary.fleetSummary}
          />
        </div>
      )}

      {/* Historical Performance & Health Trends */}
      <div className="space-y-4">
        <PerformanceLineTrendCard />
        <HealthTrendCard />
      </div>

      {/* Comprehensive Club Horse Directory Table */}
      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <h2 className="text-base font-semibold text-[var(--color-text-primary)]">
            Club Horse Roster
          </h2>
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
        </div>

        <DataTable
          ariaLabel="All horses in the club"
          rows={visibleHorses}
          columns={columns}
          getRowKey={(horse) => horse.horseId}
          loading={loading}
          pageSize={8}
          getSearchText={(horse) => `${horse.horseName} ${horse.courseName} ${horse.stallCode ?? ''} ${horse.breed ?? ''}`}
          searchPlaceholder="Search horses by name, stall, course..."
          emptyTitle={horses.length === 0 ? 'No horses registered in the club' : 'No horses match the selected filters'}
        />
      </div>
    </ScreenLayout>
  );
}
