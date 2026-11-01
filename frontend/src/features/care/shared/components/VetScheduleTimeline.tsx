'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { FilterChips, Select, Input, PageHeader, MetricCard, Notice } from '@/components/ui';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { useAuth } from '@/context/AuthContext';
import { admissionsApi } from '@/features/admissions/services/api';
import { stableApi } from '@/features/stable/services/stableService';
import { formatDate, formatLocalDateIso } from '@/lib/display';
import type { CareSchedule, CareType, CareScheduleStatus } from '@/features/admissions/types';
import type { Horse, StableStall } from '@/features/stable/types';
import {
  WORKDAY_START,
  WORKDAY_END,
  WORKDAY_LEN,
  VET_WINDOW_START,
  VET_WINDOW_END,
  computeTimelinePosition,
  formatClockLabel,
  getCareTypeTone,
  isOutsideWorkday,
  assignTimelineLanes,
  timelineRulerMarks,
} from '@/features/schedule/utils/timelineUtils';

/** "14:00" from a backend LocalDateTime string, on the 24-hour clock. */
function clockLabel(iso: string | null | undefined): string {
  if (!iso) return '';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? '' : formatClockLabel(d.getHours() * 60 + d.getMinutes());
}

export function VetScheduleTimeline() {
  const router = useRouter();
  const { user } = useAuth();
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    return formatLocalDateIso();
  });
  const [schedules, setSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [careTypeFilter, setCareTypeFilter] = useState<CareType | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');

  const loadData = useCallback(async () => {
    try {
      await Promise.resolve();
      setLoading(true);
      setError(null);

      // The vet works only on the schedules assigned to them.
      const [schedulePage, horseList, stallList] = await Promise.all([
        admissionsApi.getCareSchedules({ date: selectedDate, size: 50, veterinarianId: user?.userId }),
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
  }, [selectedDate, user]);

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

  // Overlapping cases get separate lanes so every block stays visible and clickable.
  const blockLanes = useMemo(
    () =>
      assignTimelineLanes(
        schedules
          .filter((s) => s.scheduledAt)
          .map((s) => {
            const d = new Date(s.scheduledAt as string);
            const start = d.getHours() * 60 + d.getMinutes();
            return { id: s.id, start, end: start + (s.durationMinutes || 30) };
          }),
      ),
    [schedules],
  );
  const laneHeight = blockLanes.lanes === 1 ? 56 : 44;
  const trackHeight = 8 + blockLanes.lanes * laneHeight + (blockLanes.lanes - 1) * 4;

  const handleOpenSchedule = (item: CareSchedule) => {
    if (item.careType === 'INITIAL') {
      router.push(`/veterinarian/admissions?scheduleId=${item.id}`);
    } else if (item.careType === 'ROUTINE') {
      router.push(`/veterinarian/routine/${item.id}`);
    } else if (item.careType === 'URGENT') {
      router.push(`/veterinarian/urgent/${item.id}`);
    } else {
      router.push(`/veterinarian/schedule/${item.id}`);
    }
  };

  const handleDateShift = (days: number) => {
    const [y, m, d] = selectedDate.split('-').map(Number);
    const target = new Date(y, m - 1, d + days);
    setSelectedDate(formatLocalDateIso(target));
  };

  const handleJumpToToday = () => {
    setSelectedDate(formatLocalDateIso());
  };

  return (
    <div className="space-y-6">
      {/* Top Header & Date Navigation */}
      <PageHeader
        eyebrow="Clinical Timeline"
        title="Veterinary Schedule & Timeline"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-0.5 shadow-sm">
              <Button
                variant="tertiary"
                size="sm"
                icon="chevron-left"
                onClick={() => handleDateShift(-1)}
                className="transition-all active:scale-[0.98]"
              >
                Previous
              </Button>
              <Button
                variant="tertiary"
                size="sm"
                onClick={handleJumpToToday}
                className="transition-all active:scale-[0.98]"
              >
                Today
              </Button>
              <Button
                variant="tertiary"
                size="sm"
                icon="chevron-right"
                onClick={() => handleDateShift(1)}
                className="transition-all active:scale-[0.98]"
              >
                Next
              </Button>
            </div>

            <Input
              type="date"
              aria-label="Schedule date"
              value={selectedDate}
              onChange={(e) => setSelectedDate(e.target.value)}
              className="w-auto text-xs"
            />

            <Button
              variant="secondary"
              size="sm"
              icon="refresh"
              onClick={loadData}
              disabled={loading}
              className="transition-all active:scale-[0.98]"
            >
              Refresh
            </Button>
          </div>
        }
      />

      {/* Metric Cards Banner */}
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <MetricCard
          label="Total Scheduled"
          value={stats.total}
          icon="calendar"
          tone={stats.total > 0 ? 'default' : 'neutral'}
        />
        <MetricCard
          label="Urgent Priority"
          value={stats.urgent}
          icon="alert-triangle"
          tone={stats.urgent > 0 ? 'danger' : 'neutral'}
        />
        <MetricCard
          label="Admission Quarantine"
          value={stats.initial}
          icon="stethoscope"
          tone={stats.initial > 0 ? 'info' : 'neutral'}
        />
        <MetricCard
          label="Routine & Preventive"
          value={stats.routine}
          icon="clock"
          tone={stats.routine > 0 ? 'warning' : 'neutral'}
        />
      </div>

      {error && (
        <Notice tone="error">
          <div className="flex items-center justify-between gap-3">
            <span>{error}</span>
            <Button variant="link" size="sm" onClick={loadData}>
              Retry
            </Button>
          </div>
        </Notice>
      )}



      {/* Visual Timeline Bar (08:00 - 17:00, routine window highlighted) */}
      <Panel padded className="space-y-3">
        <div className="flex items-center justify-between text-sm font-semibold">
          <span className="text-[var(--color-text-primary)]">
            Daily Timeline: {formatClockLabel(WORKDAY_START)} – {formatClockLabel(WORKDAY_END)} ({formatDate(selectedDate)})
          </span>
          <span className="text-[var(--color-text-secondary)]">
            Scheduled: <strong>{stats.totalMinutes} min</strong> / {WORKDAY_LEN} min
          </span>
        </div>

        {/* Ruler */}
        <div className="relative h-5 border-b border-[var(--color-border)] text-xs font-mono text-[var(--color-text-muted)]">
          {timelineRulerMarks().map((mark) => (
            <span
              key={mark.minutes}
              className={`absolute ${
                mark.percent === 0 ? 'left-0' : mark.percent === 100 ? 'right-0' : '-translate-x-1/2'
              }`}
              style={mark.percent !== 0 && mark.percent !== 100 ? { left: `${mark.percent}%` } : undefined}
            >
              {mark.label}
            </span>
          ))}
        </div>

        {/* Routine window indicator strip */}
        <div className="relative h-6" aria-label="Routine examination window">
          <div
            className="absolute inset-y-0 flex items-center justify-center gap-1.5 truncate rounded-[var(--radius-xs)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] px-2 text-xs font-semibold text-[var(--color-warning)] shadow-2xs"
            style={{
              left: `${((VET_WINDOW_START - WORKDAY_START) / WORKDAY_LEN) * 100}%`,
              width: `${((VET_WINDOW_END - VET_WINDOW_START) / WORKDAY_LEN) * 100}%`,
            }}
          >
            <Icon name="clock" size={12} className="shrink-0" />
            <span className="truncate">
              Routine {formatClockLabel(VET_WINDOW_START)} – {formatClockLabel(VET_WINDOW_END)}
            </span>
          </div>
        </div>

        {/* Timeline track with blocks */}
        <div
          className="relative w-full rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] overflow-hidden"
          style={{ height: `${trackHeight}px` }}
        >
          {/* Routine window band */}
          <div
            aria-hidden
            className="absolute inset-y-0 border-x border-[var(--color-warning)]/60 bg-[var(--color-warning-soft)]/30"
            style={{
              left: `${((VET_WINDOW_START - WORKDAY_START) / WORKDAY_LEN) * 100}%`,
              width: `${((VET_WINDOW_END - VET_WINDOW_START) / WORKDAY_LEN) * 100}%`,
            }}
          />
          {schedules.map((cs) => {
            if (!cs.scheduledAt) return null;
            const { leftPercent, widthPercent } = computeTimelinePosition(
              cs.scheduledAt,
              cs.durationMinutes || 30
            );
            const tone = getCareTypeTone(cs.careType, cs.purpose);
            const horse = horseMap.get(cs.horseId);
            const horseName = horse?.name || `Horse #${cs.horseId}`;
            const offHours = isOutsideWorkday(cs.scheduledAt);
            const lane = blockLanes.laneOf.get(cs.id) ?? 0;
            const blockLabel = `#${cs.id} · ${tone.label} - ${horseName} (${clockLabel(cs.scheduledAt)})${offHours ? ' · outside 08:00-17:00' : ''} · Open case`;

            return (
              <div
                key={cs.id}
                role="button"
                tabIndex={0}
                aria-label={blockLabel}
                title={blockLabel}
                onClick={() => handleOpenSchedule(cs)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleOpenSchedule(cs);
                  }
                }}
                style={{
                  left: `${leftPercent}%`,
                  width: `${widthPercent}%`,
                  top: `${4 + lane * (laneHeight + 4)}px`,
                  height: `${laneHeight}px`,
                }}
                className={`absolute flex flex-col justify-center overflow-hidden rounded border px-2 py-0.5 cursor-pointer shadow-sm transition-transform hover:scale-[1.02] hover:ring-1 hover:ring-[var(--color-border-strong)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)] ${tone.bg} ${tone.border} ${tone.text}`}
              >
                <div className="flex items-center gap-1 truncate text-xs font-bold">
                  <Icon name={offHours ? 'flag' : tone.icon} size={12} className="shrink-0" />
                  {horseName}
                </div>
                <div className="truncate text-xs opacity-80">
                  {clockLabel(cs.scheduledAt)} · {cs.durationMinutes || 30}m
                </div>
              </div>
            );
          })}
        </div>
      </Panel>

      {/* Filter Tabs */}
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <FilterChips
          label="Care type"
          value={careTypeFilter}
          onChange={(v) => setCareTypeFilter(v as CareType | 'ALL')}
          options={[
            { value: 'ALL', label: 'All', count: schedules.length },
            { value: 'URGENT', label: 'Urgent', count: stats.urgent },
            { value: 'INITIAL', label: 'Admission', count: stats.initial },
            { value: 'ROUTINE', label: 'Routine', count: stats.routine },
          ]}
        />

        <div>
          <Select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as CareScheduleStatus | 'ALL')}
            aria-label="Status"
          >
            <option value="ALL">All Statuses</option>
            <option value="SCHEDULED">Scheduled</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="COMPLETED">Completed</option>
          </Select>
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
            description={`No examinations scheduled for ${formatDate(selectedDate)}.`}
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
            const tone = getCareTypeTone(cs.careType, cs.purpose);
            const timeStr = cs.scheduledAt ? clockLabel(cs.scheduledAt) : 'Unscheduled';

            const isCompleted = cs.status === 'COMPLETED';

            return (
              <Panel
                key={cs.id}
                padded
                onClick={() => handleOpenSchedule(cs)}
                className="cursor-pointer flex flex-col justify-between border border-[var(--color-border)] transition-all duration-200 ease-out hover:-translate-y-0.5 hover:shadow-md hover:border-[var(--color-border-strong)]"
              >
                <div>
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-start gap-3 min-w-0 flex-1">
                      <HorseAvatar name={horseName} image={horse?.imageUrl} size={42} rounded="md" />
                      <div className="min-w-0 flex-1">
                        <div className="flex flex-wrap items-center gap-1.5">
                          <span className="text-base font-bold text-[var(--color-text-primary)] truncate">
                            {horseName}
                          </span>
                          <span className="text-[11px] font-mono text-[var(--color-text-muted)] bg-[var(--color-surface-muted)] px-1.5 py-0.5 rounded-[var(--radius-xs)] border border-[var(--color-border)]">
                            #{cs.horseId}
                          </span>
                          {horse?.breed && (
                            <span className="text-xs text-[var(--color-text-muted)] truncate">
                              ({horse.breed})
                            </span>
                          )}
                        </div>
                        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                          <span className="inline-flex items-center gap-1">
                            <Icon name="clock" size={12} className="text-[var(--color-primary)]" />
                            <strong>{timeStr}</strong>
                            <span className="text-[var(--color-text-muted)]">({cs.durationMinutes || 30}m)</span>
                          </span>
                          <span>·</span>
                          <span className="inline-flex items-center gap-1">
                            <Icon name="building" size={12} />
                            <span>Stall: <strong>{stallCode}</strong></span>
                          </span>
                        </div>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-1 shrink-0">
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
                        {cs.status === 'SCHEDULED'
                          ? 'Scheduled'
                          : cs.status === 'IN_PROGRESS'
                          ? 'In Progress'
                          : 'Completed'}
                      </Pill>
                    </div>
                  </div>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-[var(--color-border)] pt-3">
                  <span className="text-[11px] font-mono text-[var(--color-text-muted)]">
                    Schedule #{cs.id}
                  </span>
                  <Button
                    variant={
                      isCompleted
                        ? 'secondary'
                        : cs.careType === 'URGENT'
                        ? 'destructive'
                        : 'primary'
                    }
                    size="sm"
                    icon={
                      isCompleted
                        ? 'file-text'
                        : cs.status === 'IN_PROGRESS'
                        ? 'activity'
                        : 'chevron-right'
                    }
                    onClick={(e) => {
                      e.stopPropagation();
                      handleOpenSchedule(cs);
                    }}
                    className="transition-all active:scale-[0.97]"
                  >
                    {isCompleted
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

    </div>
  );
}
