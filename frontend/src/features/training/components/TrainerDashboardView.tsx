'use client';

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
              <span className="h-2 w-2 rounded-full bg-emerald-500" />
              Excellent (≥8.0): <strong>{performanceDist.excellent}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-blue-500" />
              On target (6.0–7.9): <strong>{performanceDist.good}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-secondary)]">
              <span className="h-2 w-2 rounded-full bg-amber-500" />
              Needs improvement (&lt;6.0): <strong>{performanceDist.needImprovement}</strong>
            </span>
            <span className="flex items-center gap-1.5 text-[var(--color-text-muted)]">
              <span className="h-2 w-2 rounded-full bg-gray-300" />
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
                className="bg-emerald-500 transition-all"
                title={`Excellent: ${performanceDist.excellent}`}
              />
            )}
            {performanceDist.good > 0 && (
              <div
                style={{ width: `${(performanceDist.good / totalHorses) * 100}%` }}
                className="bg-blue-500 transition-all"
                title={`On target: ${performanceDist.good}`}
              />
            )}
            {performanceDist.needImprovement > 0 && (
              <div
                style={{ width: `${(performanceDist.needImprovement / totalHorses) * 100}%` }}
                className="bg-amber-500 transition-all"
                title={`Needs improvement: ${performanceDist.needImprovement}`}
              />
            )}
            {performanceDist.noData > 0 && (
              <div
                style={{ width: `${(performanceDist.noData / totalHorses) * 100}%` }}
                className="bg-gray-300 transition-all"
                title={`No data: ${performanceDist.noData}`}
              />
            )}
          </div>
        )}
      </Panel>

      {/* Table Section with Filter & Search */}
      <Panel padded className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setFilterMode('ALL')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALL'
                  ? 'bg-[var(--color-primary)] text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
              }`}
            >
              All ({totalHorses})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('ACTIVE')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ACTIVE'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
              }`}
            >
              In training ({activeCount})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('ALERT')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALERT'
                  ? 'bg-red-600 text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
              }`}
            >
              With alerts ({alertHorsesCount})
            </button>
          </div>

          <div className="flex items-center gap-2">
            <div className="relative">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search horses..."
                className="w-full max-w-full rounded border border-[var(--color-border)] bg-[var(--color-surface)] py-1 pl-7 pr-2.5 text-xs text-[var(--color-text-primary)] placeholder-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none sm:w-56"
              />
              <span className="pointer-events-none absolute left-2 top-1.5 text-[var(--color-text-muted)]">
                <Icon name="search" size={12} />
              </span>
            </div>
          </div>
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
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-[var(--color-border)] text-[var(--color-text-secondary)]">
                  <th className="py-3 px-3 font-semibold">Horse</th>
                  <th className="py-3 px-3 font-semibold">Training course</th>
                  <th className="py-3 px-3 font-semibold">Status</th>
                  <th className="py-3 px-3 font-semibold">Course progress</th>
                  <th className="py-3 px-3 font-semibold text-center">Latest performance</th>
                  <th className="py-3 px-3 font-semibold text-center">30-day average</th>
                  <th className="py-3 px-3 font-semibold text-center">Alerts</th>
                  <th className="py-3 px-3 font-semibold text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[var(--color-border)] text-[var(--color-text-primary)]">
                {filteredHorses.map((horse) => {
                  return (
                    <tr
                      key={horse.horseId}
                      className="hover:bg-[var(--color-surface-subtle)] transition-colors"
                    >
                      <td className="py-3 px-3 font-semibold whitespace-nowrap">
                        <Link
                          href={`/trainer/horses/${horse.horseId}`}
                          className="hover:text-[var(--color-primary)] transition-colors"
                        >
                          {horse.horseName}
                        </Link>
                      </td>
                      <td className="py-3 px-3 text-[var(--color-text-secondary)]">
                        {horse.courseName || (
                          <span className="italic text-[var(--color-text-muted)]">Not enrolled</span>
                        )}
                      </td>
                      <td className="py-3 px-3 whitespace-nowrap">
                        {horse.planStatus === 'ACTIVE' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-blue-100 text-blue-700 px-2 py-0.5 text-[10px] font-semibold">
                            <span className="h-1.5 w-1.5 rounded-full bg-blue-500" />
                            In training
                          </span>
                        )}
                        {horse.planStatus === 'UPCOMING' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 text-amber-700 px-2 py-0.5 text-[10px] font-semibold">
                            <span className="h-1.5 w-1.5 rounded-full bg-amber-500" />
                            Upcoming
                          </span>
                        )}
                        {horse.planStatus === 'COMPLETED' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 text-emerald-700 px-2 py-0.5 text-[10px] font-semibold">
                            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500" />
                            Completed
                          </span>
                        )}
                        {(horse.planStatus === 'NO_PLAN' || !horse.planStatus) && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-gray-100 text-gray-600 px-2 py-0.5 text-[10px] font-medium">
                            No plan
                          </span>
                        )}
                        {horse.planStatus === 'CANCELLED' && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-100 text-red-700 px-2 py-0.5 text-[10px] font-medium">
                            Cancelled
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 min-w-[140px]">
                        {horse.totalSessions > 0 ? (
                          <div className="space-y-1">
                            <div className="flex items-center justify-between text-[10px] text-[var(--color-text-secondary)]">
                              <span>
                                {horse.completedSessions}/{horse.totalSessions} sessions
                              </span>
                              <span className="font-semibold">{horse.progressPercent}%</span>
                            </div>
                            <div className="h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-surface-subtle)]">
                              <div
                                style={{ width: `${Math.min(100, horse.progressPercent)}%` }}
                                className="h-full bg-blue-500 rounded-full transition-all"
                              />
                            </div>
                          </div>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center">
                        {horse.latestPerformanceRating != null ? (
                          <span
                            className={`inline-block rounded px-1.5 py-0.5 font-bold ${
                              horse.latestPerformanceRating >= 8
                                ? 'bg-emerald-100 text-emerald-800'
                                : horse.latestPerformanceRating >= 6
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-amber-100 text-amber-800'
                            }`}
                          >
                            {horse.latestPerformanceRating}/10
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center">
                        {horse.avgPerformanceRating30d != null ? (
                          <span className="font-semibold text-emerald-600">
                            {horse.avgPerformanceRating30d.toFixed(1)}
                          </span>
                        ) : (
                          <span className="text-[var(--color-text-muted)]">-</span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-center">
                        {(horse.alertCount ?? horse.alertsCount ?? 0) > 0 ? (
                          <span className="inline-flex items-center gap-1 rounded-full bg-red-100 text-red-700 px-2 py-0.5 text-[10px] font-bold">
                            <Icon name="alert-triangle" size={10} />
                            {horse.alertCount ?? horse.alertsCount} alerts
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-emerald-600 text-[10px] font-medium">
                            <Icon name="check" size={12} />
                            Clear
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-right whitespace-nowrap">
                        <Link href={`/trainer/horses/${horse.horseId}`}>
                          <Button variant="secondary" size="sm">
                            <Icon name="trending-up" size={12} />
                            View fitness
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </Panel>
    </ScreenLayout>
  );
}
