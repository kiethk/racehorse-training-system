'use client';

import { useEffect, useState, useMemo } from 'react';
import Link from 'next/link';
import { trainingService } from '../services/trainingService';
import type { TrainerDashboardHorse } from '../types';
import { Panel } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageSection, ScreenLayout } from '@/components/ui/ScreenLayout';
import { Notice } from '@/components/ui/Notice';
import { ListSkeleton } from '@/components/ui/states';
import { displayError } from '@/lib/display';

export function TrainerHorseListView() {
  const [horses, setHorses] = useState<TrainerDashboardHorse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [searchTerm, setSearchTerm] = useState('');
  const [filterMode, setFilterMode] = useState<'ALL' | 'ACTIVE' | 'ALERT'>('ALL');

  const loadHorses = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await trainingService.getDashboard();
      setHorses(data);
    } catch (err: unknown) {
      setError(displayError(err, 'Unable to load the horse list.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadHorses();
  }, []);

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
    <ScreenLayout variant="list">
      <PageHeader title="Assigned horses" description="Review each horse’s profile, fitness trend and health indicators." actions={<>
          <Link href="/trainer/dashboard">
            <Button variant="secondary" size="sm">
              <Icon name="grid" size={14} />
              View dashboard
            </Button>
          </Link>
          <Button variant="secondary" size="sm" onClick={loadHorses}>
            <Icon name="refresh" size={14} />
            Refresh
          </Button>
      </>} />

      <PageSection title="Assigned horse overview">
      <Panel padded className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => setFilterMode('ALL')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALL'
                  ? 'bg-[var(--color-primary)] text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              All ({horses.length})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('ACTIVE')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ACTIVE'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              In training ({horses.filter((h) => h.planStatus === 'ACTIVE').length})
            </button>
            <button
              type="button"
              onClick={() => setFilterMode('ALERT')}
              className={`rounded px-3 py-1.5 text-xs font-medium transition-colors ${
                filterMode === 'ALERT'
                  ? 'bg-red-600 text-white shadow-sm'
                  : 'bg-[var(--color-surface-subtle)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-hover)]'
              }`}
            >
              Alerts ({horses.filter((h) => (h.alertCount ?? h.alertsCount ?? 0) > 0).length})
            </button>
          </div>

          <div className="relative">
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search horses..."
              className="w-full max-w-full rounded border border-[var(--color-border)] bg-[var(--color-surface)] py-1.5 pl-7 pr-2.5 text-xs text-[var(--color-text-primary)] placeholder-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:outline-none sm:w-60"
            />
            <span className="pointer-events-none absolute left-2 top-2 text-[var(--color-text-muted)]">
              <Icon name="search" size={12} />
            </span>
          </div>
        </div>

        {error && <Notice tone="error">{error}</Notice>}

        {loading ? (
          <ListSkeleton rows={5} />
        ) : filteredHorses.length === 0 ? (
          <div className="py-12 text-center">
            <p className="text-sm font-medium text-[var(--color-text-secondary)]">
              No horses match the selected filters.
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredHorses.map((horse) => (
              <div
                key={horse.horseId}
                className="flex flex-col justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 shadow-sm hover:border-[var(--color-border-strong)] transition-all"
              >
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <h3 className="text-base font-bold text-[var(--color-text-primary)]">
                        {horse.horseName}
                      </h3>
                      <p className="text-xs text-[var(--color-text-secondary)]">
                        {horse.courseName || 'No training course assigned'}
                      </p>
                    </div>

                    {(horse.alertCount ?? horse.alertsCount ?? 0) > 0 ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-red-100 dark:bg-red-900/40 text-red-700 dark:text-red-300 px-2 py-0.5 text-[10px] font-bold">
                        <Icon name="alert-triangle" size={10} />
                        {horse.alertCount ?? horse.alertsCount} alerts
                      </span>
                    ) : (
                      <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 dark:bg-emerald-900/40 text-emerald-700 dark:text-emerald-300 px-2 py-0.5 text-[10px] font-medium">
                        <Icon name="check" size={10} />
                        Clear
                      </span>
                    )}
                  </div>

                  {/* Progress & Stats */}
                  <div className="mt-4 space-y-2.5">
                    {horse.totalSessions > 0 ? (
                      <div>
                        <div className="flex items-center justify-between text-[11px] text-[var(--color-text-secondary)]">
                          <span>Course progress</span>
                          <span className="font-semibold text-[var(--color-text-primary)]">
                            {horse.completedSessions}/{horse.totalSessions} sessions ({horse.progressPercent}%)
                          </span>
                        </div>
                        <div className="mt-1 h-1.5 w-full overflow-hidden rounded-full bg-[var(--color-surface-subtle)]">
                          <div
                            style={{ width: `${Math.min(100, horse.progressPercent)}%` }}
                            className="h-full bg-blue-500 rounded-full"
                          />
                        </div>
                      </div>
                    ) : (
                      <div className="text-xs text-[var(--color-text-muted)] italic">
                        No sessions scheduled
                      </div>
                    )}

                    <div className="grid grid-cols-2 gap-2 border-t border-[var(--color-border-subtle)] pt-2.5 text-xs">
                      <div>
                        <span className="text-[10px] uppercase text-[var(--color-text-muted)]">
                          Latest performance
                        </span>
                        <p className="font-semibold text-[var(--color-text-primary)]">
                          {horse.latestPerformanceRating != null
                            ? `${horse.latestPerformanceRating}/10`
                            : '-'}
                        </p>
                      </div>
                      <div>
                        <span className="text-[10px] uppercase text-[var(--color-text-muted)]">
                          30-day average
                        </span>
                        <p className="font-semibold text-emerald-600">
                          {horse.avgPerformanceRating30d != null
                            ? `${horse.avgPerformanceRating30d.toFixed(1)}/10`
                            : '-'}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="mt-4 border-t border-[var(--color-border-subtle)] pt-3">
                  <Link href={`/trainer/horses/${horse.horseId}`} className="w-full">
                    <Button variant="secondary" size="sm" className="w-full justify-center">
                      <Icon name="trending-up" size={13} />
                      View fitness and alerts
                    </Button>
                  </Link>
                </div>
              </div>
            ))}
          </div>
        )}
      </Panel>
      </PageSection>
    </ScreenLayout>
  );
}
