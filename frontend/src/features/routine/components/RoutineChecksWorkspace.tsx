'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import { stableApi } from '@/features/stable/services/stableService';
import type { CareSchedule, CareScheduleStatus } from '@/features/admissions/types';
import type { Horse, StableStall } from '@/features/stable/types';
import { VetScheduleDrawer } from '@/features/schedule/components/VetScheduleDrawer';
import { CreateRoutineModal } from './CreateRoutineModal';
import { filterRoutineSchedules } from '../utils/routineValidation';

export function RoutineChecksWorkspace() {
  const [schedules, setSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Search
  const [statusFilter, setStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  // Modals & Drawers
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [activeSchedule, setActiveSchedule] = useState<CareSchedule | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const loadData = useCallback(async () => {
    try {
      await Promise.resolve();
      setLoading(true);
      setError(null);

      const [schedulePage, horseList, stallList] = await Promise.all([
        admissionsApi.getCareSchedules({ careType: 'ROUTINE', size: 100 }),
        stableApi.getHorses(),
        stableApi.getStalls(),
      ]);

      setSchedules(schedulePage.content || []);
      setHorses(horseList || []);
      setStalls(stallList || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load routine care schedules.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => {
      if (active) void loadData();
    });
    return () => {
      active = false;
    };
  }, [loadData]);

  // Lookup maps
  const horseMap = useMemo(() => new Map(horses.map((h) => [h.id, h])), [horses]);
  const stallMap = useMemo(() => new Map(stalls.map((s) => [s.id, s.stallCode])), [stalls]);

  const horseInfoMap = useMemo(() => {
    const map = new Map<number, { name: string; stallCode?: string }>();
    for (const h of horses) {
      const stallCode = h.currentStallId ? stallMap.get(h.currentStallId) : undefined;
      map.set(h.id, { name: h.name, stallCode });
    }
    return map;
  }, [horses, stallMap]);

  // Filtered schedules
  const filteredList = useMemo(() => {
    return filterRoutineSchedules(schedules, statusFilter, searchTerm, horseInfoMap);
  }, [schedules, statusFilter, searchTerm, horseInfoMap]);

  // Stats calculation
  const stats = useMemo(() => {
    let scheduled = 0;
    let inProgress = 0;
    let completed = 0;

    for (const s of schedules) {
      if (s.status === 'SCHEDULED') scheduled++;
      else if (s.status === 'IN_PROGRESS') inProgress++;
      else if (s.status === 'COMPLETED') completed++;
    }

    return { total: schedules.length, scheduled, inProgress, completed };
  }, [schedules]);

  const handleOpenSchedule = (item: CareSchedule) => {
    setActiveSchedule(item);
    setDrawerOpen(true);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-bold tracking-tight text-[var(--color-text-primary)]">
              Routine & Preventive Care
            </h1>
            <Pill tone="neutral">{stats.total} cases</Pill>
          </div>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
            Manage preventive health checks, vaccinations, deworming, and post-injury follow-ups.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <Button variant="secondary" icon="refresh" onClick={loadData} disabled={loading}>
            Refresh
          </Button>
          <Button variant="primary" icon="plus" onClick={() => setCreateModalOpen(true)}>
            Schedule Care
          </Button>
        </div>
      </div>

      {/* Error banner */}
      {error && (
        <div className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4 text-xs text-[var(--color-danger)] flex items-center justify-between">
          <span>{error}</span>
          <button type="button" onClick={loadData} className="underline font-semibold ml-2">
            Retry
          </button>
        </div>
      )}

      {/* Metrics Cards */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
            Total Cases
          </span>
          <span className="mt-1 text-2xl font-extrabold text-[var(--color-text-primary)] font-metric">
            {stats.total}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-amber-600 dark:text-amber-400">
            Scheduled
          </span>
          <span className="mt-1 text-2xl font-extrabold text-amber-600 dark:text-amber-400 font-metric">
            {stats.scheduled}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-info)]">
            In Progress
          </span>
          <span className="mt-1 text-2xl font-extrabold text-[var(--color-info)] font-metric">
            {stats.inProgress}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-success)]">
            Completed
          </span>
          <span className="mt-1 text-2xl font-extrabold text-[var(--color-success)] font-metric">
            {stats.completed}
          </span>
        </Panel>
      </div>

      {/* Filter Tabs & Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div className="flex flex-wrap items-center gap-1.5">
          <button
            type="button"
            onClick={() => setStatusFilter('ALL')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              statusFilter === 'ALL'
                ? 'bg-[var(--color-primary)] text-white'
                : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
            }`}
          >
            All ({schedules.length})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter('SCHEDULED')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              statusFilter === 'SCHEDULED'
                ? 'bg-amber-600 text-white'
                : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
            }`}
          >
            Scheduled ({stats.scheduled})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter('IN_PROGRESS')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              statusFilter === 'IN_PROGRESS'
                ? 'bg-[var(--color-info)] text-white'
                : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
            }`}
          >
            In Progress ({stats.inProgress})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter('COMPLETED')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              statusFilter === 'COMPLETED'
                ? 'bg-[var(--color-success)] text-white'
                : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
            }`}
          >
            Completed ({stats.completed})
          </button>
        </div>

        <div className="w-full sm:w-64">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search horse name, stall..."
            className="w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-1.5 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
          />
        </div>
      </div>

      {/* Routine Cards List */}
      {loading ? (
        <ListSkeleton rows={4} />
      ) : filteredList.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="calendar"
            title="No routine schedules found"
            description={
              searchTerm
                ? 'No care schedules match your search query.'
                : 'No preventive care scheduled. Click "Schedule Care" to create an appointment.'
            }
            action={
              <Button variant="secondary" size="sm" onClick={() => setCreateModalOpen(true)}>
                + Schedule Care
              </Button>
            }
          />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {filteredList.map((cs) => {
            const horse = horseMap.get(cs.horseId);
            const horseName = horse?.name || `Horse #${cs.horseId}`;
            const stallCode = horse?.currentStallId ? stallMap.get(horse.currentStallId) : 'Unassigned';
            const dateStr = cs.scheduledAt
              ? new Date(cs.scheduledAt).toLocaleDateString('en-US')
              : cs.scheduledDate || 'Unscheduled';

            return (
              <Panel key={cs.id} padded className="flex flex-col justify-between transition-shadow hover:shadow-md border border-[var(--color-border)]">
                <div>
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-base font-bold text-[var(--color-text-primary)]">
                          {horseName}
                        </span>
                        {horse?.breed && (
                          <span className="text-xs text-[var(--color-text-muted)]">({horse.breed})</span>
                        )}
                      </div>
                      <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                        <span>📅 <strong>{dateStr}</strong></span>
                        <span>·</span>
                        <span>Stall: <strong>{stallCode}</strong></span>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-1">
                      <Pill tone="neutral" icon="clock" size="sm">
                        Preventive / Routine
                      </Pill>
                      <Pill
                        tone={
                          cs.status === 'COMPLETED'
                            ? 'success'
                            : cs.status === 'IN_PROGRESS'
                            ? 'primary'
                            : 'warning'
                        }
                        size="sm"
                      >
                        {cs.status}
                      </Pill>
                    </div>
                  </div>

                  <p className="mt-3 text-xs text-[var(--color-text-secondary)] line-clamp-2">
                    {cs.description || 'No clinical notes provided.'}
                  </p>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-[var(--color-border)] pt-3">
                  <span className="text-[11px] text-[var(--color-text-muted)]">Schedule #{cs.id}</span>
                  <Button
                    variant="primary"
                    size="sm"
                    icon={cs.status === 'COMPLETED' ? 'check' : 'chevron-right'}
                    onClick={() => handleOpenSchedule(cs)}
                  >
                    {cs.status === 'COMPLETED'
                      ? 'View Record'
                      : cs.status === 'IN_PROGRESS'
                      ? 'Continue Exam'
                      : 'Start Exam'}
                  </Button>
                </div>
              </Panel>
            );
          })}
        </div>
      )}

      {/* Create Routine Modal */}
      <CreateRoutineModal
        open={createModalOpen}
        horses={horses}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={loadData}
      />

      {/* Examination Drawer */}
      <VetScheduleDrawer
        schedule={activeSchedule}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSuccess={loadData}
      />
    </div>
  );
}
