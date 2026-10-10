'use client';

import { SearchInput } from '@/components/ui/Input';
import { FilterChips } from '@/components/ui/SegmentedControl';
import { TBody, THead, Table, Td, Th, Tr } from '@/components/ui/Table';
import { Pill } from '@/components/ui/StatusBadge';
import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { trainingService } from '../services/trainingService';
import type { TrainerDashboardHorse } from '../types';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { Notice } from '@/components/ui/Notice';
import { MetricCard } from '@/components/ui/MetricCard';
import { ListSkeleton } from '@/components/ui/states';
import { displayError } from '@/lib/display';

export function TrainerDashboardView() {
  const [horses, setHorses] = useState<TrainerDashboardHorse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [filterMode, setFilterMode] = useState<'ALL' | 'ACTIVE' | 'ALERT'>('ALL');

  const loadDashboard = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await trainingService.getDashboard();
      setHorses(data);
    } catch (err: unknown) {
      setError(displayError(err, 'Unable to load training dashboard data.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDashboard();
  }, []);

  // KPIs
  const totalHorses = horses.length;
  const activeCount = horses.filter((h) => h.planStatus === 'ACTIVE').length;
  const alertHorsesCount = horses.filter((h) => (h.alertCount ?? h.alertsCount ?? 0) > 0).length;

  const validRatings = horses
    .map((h) => h.avgPerformanceRating30d)
    .filter((r): r is number => r != null);
  const avgTeamPerformance =
    validRatings.length > 0
      ? (validRatings.reduce((sum, r) => sum + r, 0) / validRatings.length).toFixed(1)
      : null;

  // Distribution
  const performanceDist = useMemo(() => {
    let excellent = 0; // >= 8
    let good = 0; // >= 6 & < 8
    let needImprovement = 0; // < 6
    let noData = 0;

    horses.forEach((h) => {
      const rating = h.avgPerformanceRating30d ?? h.latestPerformanceRating;
      if (rating == null) {
        noData++;
      } else if (rating >= 8) {
        excellent++;
      } else if (rating >= 6) {
        good++;
      } else {
        needImprovement++;
      }
    });

    return { excellent, good, needImprovement, noData };
  }, [horses]);

  // Filtered horses
  const filteredHorses = useMemo(() => {
    return horses.filter((h) => {
      const matchSearch = h.horseName.toLowerCase().includes(searchTerm.toLowerCase());
      if (!matchSearch) return false;

      if (filterMode === 'ACTIVE') return h.planStatus === 'ACTIVE';
      if (filterMode === 'ALERT') return (h.alertCount ?? h.alertsCount ?? 0) > 0;
      return true;
    });
  }, [horses, searchTerm, filterMode]);

  return (
    <ScreenLayout variant="dashboard">
      <PageHeader
        title="Fitness and training overview"
        description="Track training progress across your stable and identify overtraining risks early."
        actions={<>
          <Link href="/trainer/plans">
            <Button variant="secondary" size="sm">
              <Icon name="clipboard" size={14} />
              Manage plans
            </Button>
          </Link>
          <Button variant="secondary" size="sm" onClick={loadDashboard}>
            <Icon name="refresh" size={14} />
            Refresh
          </Button>
        </>}
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 gap-4 min-[480px]:grid-cols-2 lg:grid-cols-4">
        <MetricCard label="Total horses" value={totalHorses} icon="horse" hint="Assigned stable" />
        <MetricCard label="In training" value={activeCount} icon="activity" tone="info" unit="active plans" hint="With scheduled sessions" />
        <MetricCard label="Risk alerts" value={alertHorsesCount} icon="alert-triangle" tone={alertHorsesCount > 0 ? 'danger' : 'success'} unit="horses" hint="Horses to monitor" />
        <MetricCard label="Average performance (30 days)" value={avgTeamPerformance ? `${avgTeamPerformance}/10` : '—'} icon="trending-up" tone="success" hint="Across all assigned horses" />
      </div>

      {/* Performance Distribution Bar */}
      <Panel padded className="space-y-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
            Team performance distribution
          </h3>
          <div className="flex items-center gap-4 text-xs">
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-[var(--color-success)]" />
              Excellent (≥8.0): <strong>{performanceDist.excellent}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-[var(--color-info)]" />
              On target (6.0–7.9): <strong>{performanceDist.good}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-[var(--color-warning)]" />
              Needs improvement (&lt;6.0): <strong>{performanceDist.needImprovement}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-muted)]">
              <span className="h-2 w-2 rounded-full bg-[var(--color-border-strong)]" />
              No data: <strong>{performanceDist.noData}</strong>
            </span>
          </div>
        </div>

        {/* Stacked distribution bar */}
        {totalHorses > 0 && (
          <div className="flex h-3 w-full overflow-hidden rounded-full bg-[var(--color-surface-subtle)]">
            {performanceDist.excellent > 0 && (
              <div
                style={{ width: `${(performanceDist.excellent / totalHorses) * 100}%` }}
                className="bg-[var(--color-success)] transition-all"
                title={`Excellent: ${performanceDist.excellent}`}
              />
            )}
            {performanceDist.good > 0 && (
              <div
                style={{ width: `${(performanceDist.good / totalHorses) * 100}%` }}
                className="bg-[var(--color-info)] transition-all"
                title={`On target: ${performanceDist.good}`}
              />
            )}
            {performanceDist.needImprovement > 0 && (
              <div
                style={{ width: `${(performanceDist.needImprovement / totalHorses) * 100}%` }}
                className="bg-[var(--color-warning)] transition-all"
                title={`Needs improvement: ${performanceDist.needImprovement}`}
              />
            )}
            {performanceDist.noData > 0 && (
              <div
                style={{ width: `${(performanceDist.noData / totalHorses) * 100}%` }}
                className="bg-[var(--color-border-strong)] transition-all"
                title={`No data: ${performanceDist.noData}`}
              />
            )}
          </div>
        )}
      </Panel>

      {/* Table Section with Filter & Search */}
      <Panel padded className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <FilterChips
            label="Horse filter"
            value={filterMode}
            onChange={setFilterMode}
            options={[
              { value: 'ALL', label: 'All', count: totalHorses },
              { value: 'ACTIVE', label: 'In training', count: activeCount },
              { value: 'ALERT', label: 'With alerts', count: alertHorsesCount },
            ]}
          />
          <SearchInput className="w-full sm:w-64" value={searchTerm} onChange={setSearchTerm} placeholder="Search horses..." />
        </div>

        {error && (
          <Notice tone="error">{error}</Notice>
        )}

        {loading ? (
          <ListSkeleton rows={5} />
        ) : filteredHorses.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-sm font-medium text-[var(--color-text-secondary)]">
              No horses match the selected filters.
            </p>
          </div>
        ) : (
          <Table bare>
              <THead>
                <Tr className="border-b border-[var(--color-border)] text-[var(--color-text-secondary)]">
                  <Th className="py-3 px-3 font-semibold">Horse</Th>
                  <Th className="py-3 px-3 font-semibold">Training course</Th>
                  <Th className="py-3 px-3 font-semibold">Status</Th>
                  <Th className="py-3 px-3 font-semibold">Course progress</Th>
                  <Th className="py-3 px-3 font-semibold text-center">Latest performance</Th>
                  <Th className="py-3 px-3 font-semibold text-center">30-day average</Th>
                  <Th className="py-3 px-3 font-semibold text-center">Alerts</Th>
                  <Th className="py-3 px-3 font-semibold text-right">Actions</Th>
                </Tr>
              </THead>
              <TBody>
                {filteredHorses.map((horse) => {
                  return (
                    <Tr
                      key={horse.horseId}
                      className="hover:bg-[var(--color-surface-subtle)] transition-colors"
                    >
                      <Td className="py-3 px-3 font-semibold whitespace-nowrap">
                        <Link
                          href={`/trainer/horses/${horse.horseId}`}
                          className="hover:text-[var(--color-primary)] transition-colors"
                        >
                          {horse.horseName}
                        </Link>
                      </Td>
                      <Td className="py-3 px-3 text-[var(--color-text-secondary)]">
                        {horse.courseName || (
                          <span className="italic text-[var(--color-text-muted)]">Not enrolled</span>
                        )}
                      </Td>
                      <Td className="py-3 px-3 whitespace-nowrap">
                        {horse.planStatus === 'ACTIVE' && (
                          <Pill tone="info" size="sm">In training</Pill>
                        )}
                        {horse.planStatus === 'UPCOMING' && (
                          <Pill tone="warning" size="sm">Upcoming</Pill>
                        )}
                        {horse.planStatus === 'COMPLETED' && (
                          <Pill tone="success" size="sm">Completed</Pill>
                        )}
                        {(horse.planStatus === 'NO_PLAN' || !horse.planStatus) && (
                          <Pill tone="neutral" size="sm">No plan</Pill>
                        )}
                        {horse.planStatus === 'CANCELLED' && (
                          <Pill tone="danger" size="sm">Cancelled</Pill>
                        )}
                      </Td>
                      <Td className="py-3 px-3 min-w-[140px]">
                        {horse.totalSessions > 0 ? (
                          <div className="space-y-1">
                            <div className="flex items-center justify-between text-xs text-[var(--color-text-secondary)]">
                              <span>
                                {horse.completedSessions}/{horse.totalSessions} sessions
                              </span>
                              <span className="font-semibold">{horse.progressPercent}%</span>
                            </div>
                            <div className="h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-surface-subtle)]">
                              <div
                                style={{ width: `${Math.min(100, horse.progressPercent)}%` }}
                                className="h-full bg-[var(--color-info)] rounded-full transition-all"
                              />
                            </div>
                          </div>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </Td>
                      <Td className="py-3 px-3 text-center">
                        {horse.latestPerformanceRating != null ? (
                          <span
                            className={`inline-block rounded px-1.5 py-0.5 font-bold ${
                              horse.latestPerformanceRating >= 8
                                ? 'bg-[var(--color-success-soft)] text-[var(--color-success)]'
                                : horse.latestPerformanceRating >= 6
                                ? 'bg-[var(--color-info-soft)] text-[var(--color-info)]'
                                : 'bg-[var(--color-warning-soft)] text-[var(--color-warning)]'
                            }`}
                          >
                            {horse.latestPerformanceRating}/10
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </Td>
                      <Td className="py-3 px-3 text-center">
                        {horse.avgPerformanceRating30d != null ? (
                          <span className="font-semibold text-[var(--color-success)]">
                            {horse.avgPerformanceRating30d.toFixed(1)}
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </Td>
                      <Td className="py-3 px-3 text-center">
                        {(horse.alertCount ?? horse.alertsCount ?? 0) > 0 ? (
                          <Pill tone="danger" size="sm" icon="alert-triangle">{horse.alertCount ?? horse.alertsCount} alerts</Pill>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-[var(--color-success)] text-xs font-medium">
                            <Icon name="check" size={12} />
                            Clear
                          </span>
                        )}
                      </Td>
                      <Td className="py-3 px-3 text-right whitespace-nowrap">
                        <Link href={`/trainer/horses/${horse.horseId}`}>
                          <Button variant="secondary" size="sm">
                            <Icon name="trending-up" size={12} />
                            View fitness
                          </Button>
                        </Link>
                      </Td>
                    </Tr>
                  );
                })}
              </TBody>
            </Table>
        )}
      </Panel>
    </ScreenLayout>
  );
}
