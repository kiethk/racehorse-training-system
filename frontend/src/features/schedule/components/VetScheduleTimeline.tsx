'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import { stableApi } from '@/features/stable/services/stableService';
import type { CareSchedule, CareType, CareScheduleStatus } from '@/features/admissions/types';
import type { Horse, StableStall } from '@/features/stable/types';
import {
  computeTimelinePosition,
  getCareTypeTone,
  VET_WINDOW_LEN,
} from '../utils/timelineUtils';
import { VetScheduleDrawer } from './VetScheduleDrawer';

export function VetScheduleTimeline() {
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    const today = new Date();
    return today.toISOString().split('T')[0];
  });
  const [schedules, setSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [careTypeFilter, setCareTypeFilter] = useState<CareType | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');

  // Drawer
  const [activeSchedule, setActiveSchedule] = useState<CareSchedule | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  const loadData = useCallback(async () => {
    try {
      await Promise.resolve();
      setLoading(true);
      setError(null);

      const [schedulePage, horseList, stallList] = await Promise.all([
        admissionsApi.getCareSchedules({ date: selectedDate, size: 50 }),
        stableApi.getHorses(),
        stableApi.getStalls(),
      ]);

      setSchedules(schedulePage.content || []);
      setHorses(horseList || []);
      setStalls(stallList || []);
    } catch (err) {
      console.error('Failed to load schedule:', err);
      setError(err instanceof Error ? err.message : 'Failed to load veterinary schedule for the selected date.');
    } finally {
      setLoading(false);
    }
  }, [selectedDate]);

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

  // Sorted and filtered schedules
  const filteredSchedules = useMemo(() => {
    return schedules
      .filter((s) => (careTypeFilter === 'ALL' ? true : s.careType === careTypeFilter))
      .filter((s) => (statusFilter === 'ALL' ? true : s.status === statusFilter))
      .sort((a, b) => new Date(a.scheduledAt || 0).getTime() - new Date(b.scheduledAt || 0).getTime());
  }, [schedules, careTypeFilter, statusFilter]);

  // Quick stats
  const stats = useMemo(() => {
    let urgent = 0;
    let initial = 0;
    let routine = 0;
    let totalMinutes = 0;

    for (const s of schedules) {
      if (s.careType === 'URGENT') urgent++;
      if (s.careType === 'INITIAL') initial++;
      if (s.careType === 'ROUTINE') routine++;
      totalMinutes += s.durationMinutes || 30;
    }

    return { total: schedules.length, urgent, initial, routine, totalMinutes };
  }, [schedules]);

  const handleOpenSchedule = (item: CareSchedule) => {
    setActiveSchedule(item);
    setDrawerOpen(true);
  };

  const handleDateShift = (days: number) => {
    const current = new Date(selectedDate);
    current.setDate(current.getDate() + days);
    setSelectedDate(current.toISOString().split('T')[0]);
  };

  const handleJumpToToday = () => {
    setSelectedDate(new Date().toISOString().split('T')[0]);
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Date Navigation */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
            Veterinary Schedule & Timeline
          </h1>
          <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
            Daily schedule of emergency cases, routine checkups, and admission examinations.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button variant="secondary" size="sm" icon="chevron-left" onClick={() => handleDateShift(-1)}>
            Previous
          </Button>
          <Button variant="secondary" size="sm" onClick={handleJumpToToday}>
            Today
          </Button>
          <input
            type="date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-xs text-[var(--color-text-primary)] outline-none focus:border-[var(--color-primary)]"
          />
          <Button variant="secondary" size="sm" icon="chevron-right" onClick={() => handleDateShift(1)}>
            Next
          </Button>
          <Button variant="secondary" size="sm" icon="refresh" onClick={loadData} disabled={loading}>
            Refresh
          </Button>
        </div>
      </div>

      {error && (
        <div className="p-4 rounded-lg bg-red-50 text-red-700 dark:bg-red-950/40 dark:text-red-300 border border-red-200 dark:border-red-900/50 text-sm">
          {error}
        </div>
      )}

      {/* Summary Metrics Chips */}
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
            Total Cases
          </span>
          <span className="mt-1 text-lg font-bold text-[var(--color-text-primary)] font-metric">
            {stats.total}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-danger)]">
            Urgent Cases
          </span>
          <span className="mt-1 text-lg font-bold text-[var(--color-danger)] font-metric">
            {stats.urgent}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-info)]">
            Initial Admission
          </span>
          <span className="mt-1 text-lg font-bold text-[var(--color-info)] font-metric">
            {stats.initial}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
            Routine Care
          </span>
          <span className="mt-1 text-lg font-bold text-[var(--color-text-primary)] font-metric">
            {stats.routine}
          </span>
        </Panel>
        <Panel padded className="flex flex-col py-3">
          <span className="text-[10px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]">
            Scheduled Time
          </span>
          <span className="mt-1 text-lg font-bold text-[var(--color-text-primary)] font-metric">
            {stats.totalMinutes} min
          </span>
        </Panel>
      </div>

      {/* Visual Timeline Bar (08:00 - 17:00) */}
      <Panel padded className="space-y-3">
        <div className="flex items-center justify-between text-xs font-semibold">
          <span className="text-[var(--color-text-primary)]">
            Daily Timeline: 08:00 – 17:00 ({selectedDate})
          </span>
          <span className="text-[var(--color-text-secondary)]">
            Scheduled: <strong>{stats.totalMinutes} min</strong> / {VET_WINDOW_LEN} min
          </span>
        </div>

        {/* Ruler */}
        <div className="relative h-5 border-b border-[var(--color-border)] text-[10px] font-mono text-[var(--color-text-muted)]">
          <span className="absolute left-0">08:00</span>
          <span className="absolute left-[22.2%] -translate-x-1/2">10:00</span>
          <span className="absolute left-[44.4%] -translate-x-1/2">12:00</span>
          <span className="absolute left-[66.6%] -translate-x-1/2">14:00</span>
          <span className="absolute left-[88.8%] -translate-x-1/2">16:00</span>
          <span className="absolute right-0">17:00</span>
        </div>

        {/* Timeline track with blocks */}
        <div className="relative h-16 w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] overflow-hidden">
          {schedules.map((cs) => {
            if (!cs.scheduledAt) return null;
            const { leftPercent, widthPercent } = computeTimelinePosition(
              cs.scheduledAt,
              cs.durationMinutes || 30
            );
            const tone = getCareTypeTone(cs.careType);
            const horse = horseMap.get(cs.horseId);
            const horseName = horse?.name || `Horse #${cs.horseId}`;

            return (
              <div
                key={cs.id}
                onClick={() => handleOpenSchedule(cs)}
                style={{ left: `${leftPercent}%`, width: `${widthPercent}%` }}
                className={`absolute top-1 bottom-1 flex flex-col justify-center rounded border px-2 py-0.5 cursor-pointer shadow-sm transition-transform hover:scale-[1.02] ${tone.bg} ${tone.border} ${tone.text}`}
                title={`#${cs.id} · ${tone.label} - ${horseName} (${new Date(cs.scheduledAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})`}
              >
                <div className="truncate text-[10px] font-bold">
                  {cs.careType === 'URGENT' ? '🚨 ' : cs.careType === 'INITIAL' ? '🩺 ' : '🕒 '}
                  {horseName}
                </div>
                <div className="truncate text-[9px] opacity-80">
                  {new Date(cs.scheduledAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} · {cs.durationMinutes || 30}m
                </div>
              </div>
            );
          })}
        </div>
      </Panel>

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <div className="flex flex-wrap items-center gap-1.5">
          <button
            type="button"
            onClick={() => setCareTypeFilter('ALL')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              careTypeFilter === 'ALL'
                ? 'bg-[var(--color-primary)] text-white'
                : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
            }`}
          >
            All ({schedules.length})
          </button>
          <button
            type="button"
            onClick={() => setCareTypeFilter('URGENT')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              careTypeFilter === 'URGENT'
                ? 'bg-[var(--color-danger)] text-white'
                : 'text-[var(--color-danger)] hover:bg-[var(--color-danger-soft)]'
            }`}
          >
            🚨 Urgent ({stats.urgent})
          </button>
          <button
            type="button"
            onClick={() => setCareTypeFilter('INITIAL')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              careTypeFilter === 'INITIAL'
                ? 'bg-[var(--color-info)] text-white'
                : 'text-[var(--color-info)] hover:bg-[var(--color-info-soft)]'
            }`}
          >
            🩺 Initial ({stats.initial})
          </button>
          <button
            type="button"
            onClick={() => setCareTypeFilter('ROUTINE')}
            className={`rounded-full px-3 py-1 text-xs font-semibold transition-colors ${
              careTypeFilter === 'ROUTINE'
                ? 'bg-[var(--color-surface-elevated)] text-[var(--color-text-primary)] border border-[var(--color-border-strong)]'
                : 'text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]'
            }`}
          >
            🕒 Routine ({stats.routine})
          </button>
        </div>

        <div>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as CareScheduleStatus | 'ALL')}
            className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-xs text-[var(--color-text-primary)]"
          >
            <option value="ALL">All Statuses</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
      </div>

      {/* Cards Grid */}
      {loading ? (
        <ListSkeleton rows={4} />
      ) : filteredSchedules.length === 0 ? (
        <Panel padded>
          <EmptyState
            icon="calendar"
            title="No examinations scheduled"
            description={`No examinations scheduled for ${selectedDate}.`}
            action={
              <Button variant="secondary" size="sm" onClick={handleJumpToToday}>
                View Today
              </Button>
            }
          />
        </Panel>
      ) : (
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          {filteredSchedules.map((cs) => {
            const horse = horseMap.get(cs.horseId);
            const horseName = horse?.name || `Horse #${cs.horseId}`;
            const stallCode = horse?.currentStallId ? stallMap.get(horse.currentStallId) : 'Unassigned';
            const tone = getCareTypeTone(cs.careType);
            const timeStr = cs.scheduledAt
              ? new Date(cs.scheduledAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
              : 'Unscheduled';

            return (
              <Panel key={cs.id} padded className="flex flex-col justify-between transition-shadow hover:shadow-md">
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
                        <span>⏰ <strong>{timeStr}</strong> ({cs.durationMinutes || 30} min)</span>
                        <span>·</span>
                        <span>Stall: <strong>{stallCode}</strong></span>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-1">
                      <Pill tone={tone.pillTone} icon={tone.icon} size="sm">
                        {tone.label}
                      </Pill>
                      <Pill
                        tone={
                          cs.status === 'COMPLETED'
                            ? 'success'
                            : cs.status === 'IN_PROGRESS'
                            ? 'primary'
                            : 'neutral'
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
                    variant={cs.careType === 'URGENT' ? 'destructive' : 'primary'}
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

      {/* Slide-out Examination Drawer */}
      <VetScheduleDrawer
        schedule={activeSchedule}
        open={drawerOpen}
        onClose={() => setDrawerOpen(false)}
        onSuccess={loadData}
      />
    </div>
  );
}
