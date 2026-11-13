'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { getScheduleExamAction, getScheduleStatusDisplay } from '@/features/care/lib/scheduleStatusDisplay';
import { Icon } from '@/components/ui/Icon';
import { FilterChips, Select, Input, PageHeader, Notice, Tabs } from '@/components/ui';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { useAuth } from '@/context/AuthContext';
import { admissionsApi } from '@/features/admissions/services/api';
import { stableApi } from '@/features/stable/services/stableService';
import { formatDate, formatLocalDateIso } from '@/lib/display';
import type { CareSchedule, CareType, CareScheduleStatus } from '@/features/admissions/types';
import type { Horse, StableStall } from '@/features/stable/types';
import {
  getCareTypeTone,
  getMondayOf,
  getWeekDates,
} from '@/features/schedule/utils/timelineUtils';

export function VetScheduleTimeline() {
  const router = useRouter();
  const { user } = useAuth();
  const [view, setView] = useState<'today' | 'week'>('today');
  const [selectedDate, setSelectedDate] = useState<string>(() => {
    return formatLocalDateIso();
  });
  const [schedules, setSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters (Urgent excluded by rule)
  const [careTypeFilter, setCareTypeFilter] = useState<CareType | 'PREVENTIVE' | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');

  const mondayIso = useMemo(() => getMondayOf(selectedDate), [selectedDate]);
  const weekDays = useMemo(() => getWeekDates(mondayIso), [mondayIso]);

  const loadData = useCallback(async () => {
    try {
      await Promise.resolve();
      setLoading(true);
      setError(null);

      // The vet works only on the non-urgent schedules assigned to them
      const [schedulePage, horseList, stallList] = await Promise.all([
        admissionsApi.getCareSchedules({
          date: view === 'today' ? selectedDate : undefined,
          size: view === 'today' ? 50 : 200,
          veterinarianId: user?.userId,
        }),
        stableApi.getHorses(),
        stableApi.getStalls(),
      ]);

      // Rule 9: Exclude URGENT cases completely from Schedule
      const nonUrgent = (schedulePage.content || []).filter((s) => s.careType !== 'URGENT');
      setSchedules(nonUrgent);
      setHorses(horseList || []);
      setStalls(stallList || []);
    } catch (err) {
      console.error('Failed to load schedule:', err);
      setError(err instanceof Error ? err.message : 'Failed to load veterinary schedule.');
    } finally {
      setLoading(false);
    }
  }, [selectedDate, view, user]);

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

  // Today's schedules strictly matched by selectedDate to avoid any flash/leak when switching from week view
  const todaySchedules = useMemo(() => {
    return schedules.filter((s) => {
      const dateIso = s.scheduledDate || (s.scheduledAt ? s.scheduledAt.split('T')[0] : null);
      return dateIso === selectedDate;
    });
  }, [schedules, selectedDate]);

  // Sorted and filtered schedules for Today view
  const filteredSchedules = useMemo(() => {
    return todaySchedules
      .filter((s) => {
        if (careTypeFilter === 'ALL') return true;
        if (careTypeFilter === 'INITIAL') return s.careType === 'INITIAL';
        if (careTypeFilter === 'PREVENTIVE') return s.careType === 'ROUTINE' && s.purpose === 'PREVENTIVE';
        if (careTypeFilter === 'ROUTINE') return s.careType === 'ROUTINE' && s.purpose !== 'PREVENTIVE';
        return s.careType === careTypeFilter;
      })
      .filter((s) => (statusFilter === 'ALL' ? true : s.status === statusFilter))
      .sort((a, b) => new Date(a.scheduledAt || 0).getTime() - new Date(b.scheduledAt || 0).getTime());
  }, [todaySchedules, careTypeFilter, statusFilter]);

  // Quick stats computed specifically for today's selectedDate
  const stats = useMemo(() => {
    let initial = 0;
    let routine = 0;
    let preventive = 0;

    for (const s of todaySchedules) {
      if (s.careType === 'INITIAL') initial++;
      if (s.careType === 'ROUTINE') {
        if (s.purpose === 'PREVENTIVE') preventive++;
        else routine++;
      }
    }

    return { total: todaySchedules.length, initial, routine, preventive };
  }, [todaySchedules]);

  const handleOpenSchedule = (item: CareSchedule) => {
    if (item.status === 'COMPLETED') {
      router.push(`/veterinarian/records?scheduleId=${item.id}&back=schedule`);
    } else if (item.careType === 'INITIAL') {
      router.push(`/veterinarian/admissions?scheduleId=${item.id}`);
    } else if (item.purpose === 'PREVENTIVE') {
      router.push(`/veterinarian/preventive-care?horseId=${item.horseId}`);
    } else if (item.careType === 'ROUTINE') {
      router.push(`/veterinarian/routine/${item.id}`);
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
      {/* Top Header & Navigation */}
      <PageHeader
        title="Veterinary Schedule"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <div className="inline-flex rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-0.5 shadow-sm">
              <Button
                variant="tertiary"
                size="sm"
                icon="chevron-left"
                onClick={() => handleDateShift(view === 'today' ? -1 : -7)}
                className="transition-all active:scale-[0.98]"
              >
                {view === 'today' ? 'Previous' : 'Prev Week'}
              </Button>
              <Button
                variant="tertiary"
                size="sm"
                onClick={handleJumpToToday}
                className="transition-all active:scale-[0.98]"
              >
                {view === 'today' ? 'Today' : 'This Week'}
              </Button>
              <Button
                variant="tertiary"
                size="sm"
                icon="chevron-right"
                onClick={() => handleDateShift(view === 'today' ? 1 : 7)}
                className="transition-all active:scale-[0.98]"
              >
                {view === 'today' ? 'Next' : 'Next Week'}
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

      {/* Tabs navigation: Today | Week */}
      <div className="border-b border-[var(--color-border)] pb-2">
        <Tabs
          tabs={[
            { id: 'today', label: 'Today', icon: 'clock' },
            { id: 'week', label: 'Week', icon: 'calendar' },
          ]}
          active={view}
          onChange={(id) => setView(id as 'today' | 'week')}
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

      {/* View: TODAY */}
      {view === 'today' && (
        <div className="space-y-6">
          {/* Capacity Overview Bar */}
          <Panel padded className="border border-[var(--color-border)] bg-[var(--color-surface)]">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex flex-wrap items-center gap-6">
                <div>
                  <div className="text-xs font-semibold text-[var(--color-text-secondary)]">
                    Routine Care Bucket
                  </div>
                  <div className="mt-1 flex items-center gap-2">
                    <span className="text-xl font-bold text-[var(--color-text-primary)]">
                      {stats.routine}/3
                    </span>
                    <Pill tone={stats.routine >= 3 ? 'warning' : 'neutral'} size="sm">
                      Window 13:30 – 15:30
                    </Pill>
                  </div>
                </div>

                <div className="hidden sm:block h-8 w-px bg-[var(--color-border)]" />

                <div>
                  <div className="text-xs font-semibold text-[var(--color-text-secondary)]">
                    Preventive Care Bucket
                  </div>
                  <div className="mt-1 flex items-center gap-2">
                    <span className="text-xl font-bold text-[var(--color-text-primary)]">
                      {stats.preventive}/3
                    </span>
                    <Pill tone="neutral" size="sm">
                      Daytime Intake
                    </Pill>
                  </div>
                </div>

                <div className="hidden sm:block h-8 w-px bg-[var(--color-border)]" />

                <div>
                  <div className="text-xs font-semibold text-[var(--color-text-secondary)]">
                    Admission Exams
                  </div>
                  <div className="mt-1 flex items-center gap-2">
                    <span className="text-xl font-bold text-[var(--color-text-primary)]">
                      {stats.initial}/2
                    </span>
                    <Pill tone="neutral" size="sm">
                      Daytime Intake
                    </Pill>
                  </div>
                </div>
              </div>

  
            </div>
          </Panel>

          {/* Filter Tabs */}
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
            <FilterChips
              label="Care type"
              value={careTypeFilter}
              onChange={(v) => setCareTypeFilter(v as CareType | 'PREVENTIVE' | 'ALL')}
              options={[
                { value: 'ALL', label: 'All', count: schedules.length },
                { value: 'INITIAL', label: 'Admission', count: stats.initial },
                { value: 'ROUTINE', label: 'Routine', count: stats.routine },
                { value: 'PREVENTIVE', label: 'Preventive', count: stats.preventive },
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
                <option value="CANCELLED">Cancelled</option>
              </Select>
            </div>
          </div>

          {/* Cards Grid: Idea 1 Compact Card */}
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
            <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
              {filteredSchedules.map((cs) => {
                const horse = horseMap.get(cs.horseId);
                const horseName = horse?.name || `Horse #${cs.horseId}`;
                const stallCode = horse?.currentStallId ? stallMap.get(horse.currentStallId) : 'Unassigned';
                const tone = getCareTypeTone(cs.careType, cs.purpose, cs.sourceIncidentId);
                const isCompleted = cs.status === 'COMPLETED';
                const statusDisplay = getScheduleStatusDisplay(cs.status);
                const examAction = getScheduleExamAction(cs.status);

                // Routine window check (13:30 - 15:30) for ROUTINE non-preventive
                const _now = new Date();
                const todayIso = `${_now.getFullYear()}-${String(_now.getMonth() + 1).padStart(2, '0')}-${String(_now.getDate()).padStart(2, '0')}`;
                const rawDate = cs.scheduledDate || (cs.scheduledAt ? cs.scheduledAt.slice(0, 10) : null);
                const isFuture = Boolean(!isCompleted && rawDate && rawDate > todayIso);
                const _minNow = _now.getHours() * 60 + _now.getMinutes();
                const isPreventive = cs.purpose === 'PREVENTIVE';
                const isOutsideRoutineWindow =
                  cs.careType === 'ROUTINE' &&
                  !isPreventive &&
                  cs.status === 'SCHEDULED' &&
                  (_minNow < 13 * 60 + 30 || _minNow > 15 * 60 + 30);
                const canExamine = isPreventive || cs.status === 'IN_PROGRESS' || (!isFuture && !isOutsideRoutineWindow);

                return (
                  <Panel
                    key={cs.id}
                    padded
                    onClick={() => {
                      if (isCompleted || canExamine) {
                        handleOpenSchedule(cs);
                      }
                    }}
                    className={`border border-[var(--color-border)] transition-all duration-200 ease-out ${
                      isCompleted || canExamine
                        ? 'cursor-pointer hover:-translate-y-0.5 hover:shadow-md hover:border-[var(--color-border-strong)]'
                        : 'cursor-not-allowed opacity-80'
                    }`}
                  >
                    <div className="flex items-center justify-between gap-3">
                      {/* Left: Avatar + Details */}
                      <div className="flex items-center gap-3 min-w-0 flex-1">
                        <HorseAvatar name={horseName} image={horse?.imageUrl} size={42} rounded="md" />
                        <div className="min-w-0 flex-1">
                          <div className="text-base font-bold text-[var(--color-text-primary)] truncate">
                            {horseName}
                          </div>
                          <div className="mt-1 flex flex-wrap items-center gap-1.5">
                            <Pill tone={tone.pillTone} icon={tone.icon} size="sm">
                              {tone.label}
                            </Pill>
                            {cs.status !== 'SCHEDULED' && (
                              <Pill tone={statusDisplay.tone} icon={statusDisplay.icon} size="sm">
                                {statusDisplay.label}
                              </Pill>
                            )}
                            {Boolean(cs.rolloverCount && cs.rolloverCount > 0) && (
                              <Pill tone="warning" size="sm">
                                Rolled {cs.rolloverCount}x
                              </Pill>
                            )}
                            {cs.rescheduleReason === 'PEER_OFFLOAD' && (
                              <Pill tone="neutral" size="sm">
                                Transferred
                              </Pill>
                            )}
                          </div>
                          <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-secondary)]">
                            <span className="inline-flex items-center gap-1">
                              <Icon name="building" size={12} className="text-[var(--color-primary)]" />
                              <span>Stall <strong>{stallCode}</strong></span>
                            </span>
                            {cs.careType === 'INITIAL' && (
                              <>
                                <span>·</span>
                                <span className="inline-flex items-center gap-1">
                                  <Icon name="calendar" size={12} className="text-[var(--color-text-muted)]" />
                                  <span>Daytime Intake</span>
                                </span>
                              </>
                            )}
                          </div>
                        </div>
                      </div>

                      {/* Right: Action Button */}
                      <div className="shrink-0 flex flex-col items-end gap-1">
                        <Button
                          variant={isCompleted || cs.status === 'IN_PROGRESS' ? 'secondary' : 'primary'}
                          size="sm"
                          icon={examAction.icon}
                          disabled={!isCompleted && !canExamine}
                          onClick={(e) => {
                            e.stopPropagation();
                            if (isCompleted || canExamine) {
                              handleOpenSchedule(cs);
                            }
                          }}
                          className="transition-all active:scale-[0.97]"
                        >
                          {examAction.label}
                        </Button>
                        {!isCompleted && isOutsideRoutineWindow && cs.status !== 'IN_PROGRESS' && (
                          <span className="text-[11px] text-[var(--color-text-secondary)] font-medium">
                            Opens 13:30 – 15:30
                          </span>
                        )}
                      </div>
                    </div>
                  </Panel>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* View: WEEK (7-Day Calendar Grid) */}
      {view === 'week' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-sm font-semibold">
            <span className="text-[var(--color-text-primary)]">
              Week of {formatDate(mondayIso)} – {formatDate(weekDays[6])}
            </span>
          </div>

          {loading ? (
            <ListSkeleton rows={3} />
          ) : (
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-7">
              {weekDays.map((dayIso) => {
                const daySchedules = schedules.filter((s) => (s.scheduledDate ? s.scheduledDate === dayIso : s.scheduledAt?.startsWith(dayIso)));
                const routineClinical = daySchedules.filter((s) => s.careType === 'ROUTINE' && s.purpose !== 'PREVENTIVE').length;
                const prevCount = daySchedules.filter((s) => s.careType === 'ROUTINE' && s.purpose === 'PREVENTIVE').length;
                const admissionCount = daySchedules.filter((s) => s.careType === 'INITIAL').length;
                const isToday = dayIso === formatLocalDateIso();
                const [year, month, day] = dayIso.split('-').map(Number);
                const dayDate = new Date(year, month - 1, day);
                const dayLabel = formatDate(dayDate, { weekday: 'short', day: '2-digit', month: 'short' });

                return (
                  <Panel
                    key={dayIso}
                    className={`flex flex-col min-h-[300px] p-3 border transition-all duration-150 ${
                      isToday
                        ? 'border-[var(--color-primary)] ring-1 ring-[var(--color-primary)] bg-[var(--color-surface)] shadow-sm'
                        : 'border-[var(--color-border)] bg-[var(--color-surface-muted)]'
                    }`}
                  >
                    {/* Day Column Header */}
                    <div className="border-b border-[var(--color-border)] pb-2 mb-2">
                      <div className="flex items-center justify-between">
                        <div>
                          <span className="text-xs font-bold text-[var(--color-text-primary)] block">
                            {dayLabel}
                          </span>
                          {isToday && (
                            <span className="text-[10px] font-semibold text-[var(--color-primary)] uppercase tracking-wide">
                              Today
                            </span>
                          )}
                        </div>
                        <Pill tone={daySchedules.length > 0 ? 'primary' : 'neutral'} size="sm">
                          {daySchedules.length}
                        </Pill>
                      </div>
                      <div className="mt-1 flex items-center gap-1 text-[10px] text-[var(--color-text-muted)] font-medium">
                        <span>R: {routineClinical}/3</span>
                        <span>·</span>
                        <span>P: {prevCount}/3</span>
                        <span>·</span>
                        <span>A: {admissionCount}/2</span>
                      </div>
                    </div>

                    {/* Day Column Body */}
                    <div className="space-y-2 flex-1">
                      {daySchedules.length === 0 ? (
                        <p className="text-[11px] text-[var(--color-text-muted)] text-center py-10">
                          No exams
                        </p>
                      ) : (
                        daySchedules.map((cs) => {
                          const horse = horseMap.get(cs.horseId);
                          const horseName = horse?.name || `Horse #${cs.horseId}`;
                          const timeStr = cs.status === 'COMPLETED'
                            ? 'Completed'
                            : (cs.careType === 'INITIAL' ? 'Admission' : 'Routine');
                          const tone = getCareTypeTone(cs.careType, cs.purpose, cs.sourceIncidentId);

                          // Block ROUTINE SCHEDULED outside 13:30–15:30 window
                          const _now = new Date();
                          const _minNow = _now.getHours() * 60 + _now.getMinutes();
                          const isRoutineBlocked =
                            cs.careType === 'ROUTINE' &&
                            cs.purpose !== 'PREVENTIVE' &&
                            cs.status === 'SCHEDULED' &&
                            (_minNow < 13 * 60 + 30 || _minNow > 15 * 60 + 30);

                          return (
                            <div
                              key={cs.id}
                              role={isRoutineBlocked ? undefined : 'button'}
                              tabIndex={isRoutineBlocked ? undefined : 0}
                              onClick={() => { if (!isRoutineBlocked) handleOpenSchedule(cs); }}
                              onKeyDown={(e) => {
                                if (!isRoutineBlocked && (e.key === 'Enter' || e.key === ' ')) {
                                  e.preventDefault();
                                  handleOpenSchedule(cs);
                                }
                              }}
                              className={`p-2 rounded-[var(--radius-sm)] border transition-all focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-[var(--color-focus)] ${tone.bg} ${tone.border} ${tone.text} ${isRoutineBlocked ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer hover:scale-[1.02] hover:shadow-xs'}`}
                              title={isRoutineBlocked ? `Routine exam window: 13:30 – 15:30` : `#${cs.id} · ${horseName} (${timeStr}) · Click to open`}
                            >
                              <div className="flex items-center justify-between text-xs font-bold">
                                <span>{timeStr}</span>
                                <div className="flex items-center gap-1">
                                  {Boolean(cs.rolloverCount && cs.rolloverCount > 0) && (
                                    <span className="text-[9px] px-1 rounded bg-[var(--color-warning-subtle)] text-[var(--color-warning-strong)] font-bold">
                                      Rolled
                                    </span>
                                  )}
                                  <span className="text-[10px] uppercase font-semibold">{tone.label}</span>
                                </div>
                              </div>
                              <p className="text-xs truncate font-medium mt-1 text-[var(--color-text-primary)]">
                                {horseName}
                              </p>
                              {isRoutineBlocked && (
                                <p className="text-[9px] mt-1 font-semibold text-[var(--color-text-muted)]">
                                  Opens 13:30 – 15:30
                                </p>
                              )}
                            </div>
                          );
                        })
                      )}
                    </div>
                  </Panel>
                );
              })}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
