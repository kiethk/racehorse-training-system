'use client';

import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  FilterChips,
  MetricCard,
  Notice,
  PageHeader,
  ScreenLayout,
  Skeleton,
} from '@/components/ui';
import { DataTable } from '@/components/ui/DataTable';
import { displayError, formatNumber } from '@/lib/display';
import { dashboardService } from '../../services/dashboardService';
import type { DashboardHorse, ManagerDashboardSummary } from '../../types';
import { HealthTrendCard } from '../HealthTrendCard';
import { PerformanceLineTrendCard } from './PerformanceLineTrendCard';
import { BarnOccupancyWidget } from './BarnOccupancyWidget';
import { FleetDistributionWidget } from './FleetDistributionWidget';
import { ManagerActionCenter } from './ManagerActionCenter';
import { AdmissionFunnelExecutiveWidget } from './AdmissionFunnelExecutiveWidget';
import { VetWorkloadExecutiveWidget } from './VetWorkloadExecutiveWidget';
import { LazySection } from './LazySection';
import { columnsFor as horseColumnsFor, newestHorseFirst } from '../HorseDashboardView';

type HorseFilter = 'ALL' | 'ACTIVE' | 'ALERT';

export function ManagerDashboardView() {
  const [summary, setSummary] = useState<ManagerDashboardSummary | null>(null);
  const [horses, setHorses] = useState<DashboardHorse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<HorseFilter>('ALL');

  const router = useRouter();
  const horseBasePath = '/manager/horses';
  const columns = useMemo(() => horseColumnsFor(horseBasePath), [horseBasePath]);

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

  const inTraining = useMemo(() => horses.filter((h) => h.courseRunStatus === 'ACTIVE'), [horses]);
  const withAlerts = useMemo(() => horses.filter((h) => h.alertsCount > 0), [horses]);

  const visibleHorses = filter === 'ACTIVE' ? inTraining : filter === 'ALERT' ? withAlerts : horses;

  return (
    <ScreenLayout variant="dashboard">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <PageHeader title="Dashboard" />
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

      {/* Row 1: Top Executive KPI Cards */}
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

      {/* Row 2: Urgent Action Center */}
      {summary && (
        <ManagerActionCenter
          pendingAdmissions={summary.pendingAdmissions}
          urgentIncidents={summary.urgentIncidents}
        />
      )}

      {/* Row 3: Pair 1 - Stables & Fleet Readiness */}
      {summary && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <BarnOccupancyWidget
            summary={summary.stallSummary}
            barns={summary.barnOccupancies}
          />
          <FleetDistributionWidget
            summary={summary.fleetSummary}
            horses={horses}
          />
        </div>
      )}

      {/* Row 4: Pair 2 - Operations & Intake EDA (Lazy Loaded) */}
      <LazySection height="320px">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <AdmissionFunnelExecutiveWidget />
          <VetWorkloadExecutiveWidget />
        </div>
      </LazySection>

      {/* Row 5: Pair 3 - Historical Performance & Health Trends (Lazy Loaded) */}
      <LazySection height="320px">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <PerformanceLineTrendCard />
          <HealthTrendCard />
        </div>
      </LazySection>

      {/* Row 6: Comprehensive Club Horse Directory Table (Lazy Loaded) */}
      <LazySection height="420px">
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
            defaultOrder={newestHorseFirst}
            columns={columns}
            getRowKey={(horse) => horse.horseId}
            onRowClick={(horse) => router.push(`${horseBasePath}/${horse.horseId}`)}
            loading={loading}
            pageSize={8}
            getSearchText={(horse) =>
              `${horse.horseName} ${horse.courseName} ${horse.stallCode ?? ''} ${horse.breed ?? ''}`
            }
            searchPlaceholder="Search horses by name, stall, course..."
            emptyTitle={
              horses.length === 0
                ? 'No horses registered in the club'
                : 'No horses match the selected filters'
            }
          />
        </div>
      </LazySection>
    </ScreenLayout>
  );
}
