'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { FilterChips, SearchInput, Notice, PageHeader, MetricCard, Pagination } from '@/components/ui';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import { stableApi } from '@/features/stable/services/stableService';
import type { CareSchedule, CareScheduleStatus } from '@/features/admissions/types';
import type { Horse, StableStall } from '@/features/stable/types';
import { useAuth } from '@/context/AuthContext';
import { formatDate } from '@/lib/display';
import { getCareTypeTone } from '@/features/schedule/utils/timelineUtils';
import { getScheduleExamAction } from '@/features/care/lib/scheduleStatusDisplay';
import { CreateRoutineModal } from './CreateRoutineModal';
import { filterRoutineSchedules } from '@/features/routine/utils/routineValidation';

export function RoutineChecksWorkspace() {
  const router = useRouter();
  const { user } = useAuth();
  const [schedules, setSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Search
  const [statusFilter, setStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 8;

  const [createModalOpen, setCreateModalOpen] = useState(false);

  const loadData = useCallback(async () => {
    try {
      await Promise.resolve();
      setLoading(true);
      setError(null);

      // The vet works only on the schedules assigned to them.
      const [schedulePage, horseList, stallList] = await Promise.all([
        admissionsApi.getCareSchedules({ careType: 'ROUTINE', size: 100, veterinarianId: user?.userId }),
        stableApi.getHorses(),
        stableApi.getStalls(),
      ]);

      // Routine Checks tab strictly manages clinical routine exams and follow-ups.
      // Preventive care (vaccinations & deworming) is managed in the Preventive Care tab.
      const routineOnly = (schedulePage.content || []).filter((s) => s.purpose !== 'PREVENTIVE');
      setSchedules(routineOnly);
      setHorses(horseList || []);
      setStalls(stallList || []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load routine care schedules.');
    } finally {
      setLoading(false);
    }
  }, [user]);

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

  // Pagination slice
  const pageCount = Math.max(1, Math.ceil(filteredList.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount - 1);
  const paginatedList = useMemo(() => {
    const start = currentPage * PAGE_SIZE;
    return filteredList.slice(start, start + PAGE_SIZE);
  }, [filteredList, currentPage]);

  // Stats calculation
  const stats = useMemo(() => {
    let scheduled = 0;
    let completed = 0;

    for (const s of schedules) {
      if (s.status === 'SCHEDULED') scheduled++;
      else if (s.status === 'COMPLETED') completed++;
    }

    return { total: schedules.length, scheduled, completed };
  }, [schedules]);

  const handleOpenSchedule = (item: CareSchedule) => {
    if (item.status === 'COMPLETED') {
      router.push(`/veterinarian/records?scheduleId=${item.id}`);
      return;
    }
    router.push(`/veterinarian/routine/${item.id}`);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title="Routine Checks"
        actions={
          <div className="flex items-center gap-2">
            <Button
              variant="secondary"
              icon="refresh"
              onClick={loadData}
              disabled={loading}
              className="transition-all active:scale-[0.98]"
            >
              Refresh
            </Button>
            <Button
              variant="primary"
              icon="plus"
              onClick={() => setCreateModalOpen(true)}
              className="transition-all active:scale-[0.98]"
            >
              Schedule Routine Check
            </Button>
          </div>
        }
      />

      {/* Metric Cards Banner */}
      <div className="grid grid-cols-1 gap-3 sm:gap-4 sm:grid-cols-3">
        <MetricCard
          label="Total Routine Schedules"
          value={stats.total}
          icon="calendar"
          tone={stats.total > 0 ? 'default' : 'neutral'}
        />
        <MetricCard
          label="Scheduled Checks"
          value={stats.scheduled}
          icon="clock"
          tone={stats.scheduled > 0 ? 'warning' : 'neutral'}
        />
        <MetricCard
          label="Completed Checks"
          value={stats.completed}
          icon="check"
          tone={stats.completed > 0 ? 'success' : 'neutral'}
        />
      </div>

      {/* Policy Strip */}
      <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-2.5 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] text-xs text-[var(--color-text-secondary)]">
        <div className="flex items-center gap-2">
          <Icon name="clock" size={14} className="text-[var(--color-primary)] shrink-0" />
          <span>Routine Examination Window: <strong>13:30 – 15:30</strong></span>
        </div>
        <div className="flex items-center gap-2">
          <Icon name="activity" size={14} className="text-[var(--color-text-muted)] shrink-0" />
          <span>Daily quota: <strong>Max 3 routine exams per vet</strong></span>
        </div>
      </div>

      {/* Error banner */}
      {error && (
        <Notice tone="error">
          <span className="flex items-center justify-between gap-3">
            {error}
            <Button variant="link" size="sm" onClick={loadData}>
              Retry
            </Button>
          </span>
        </Notice>
      )}

      {/* Filter Tabs & Search */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
        <FilterChips
          label="Status"
          value={statusFilter}
          onChange={(v) => {
            setStatusFilter(v as CareScheduleStatus | 'ALL');
            setPage(0);
          }}
          options={[
            { value: 'ALL', label: 'All', count: schedules.length },
            { value: 'SCHEDULED', label: 'Scheduled', count: stats.scheduled },
            { value: 'COMPLETED', label: 'Completed', count: stats.completed },
          ]}
        />

        <div className="w-full sm:w-64">
          <SearchInput
            value={searchTerm}
            onChange={(term) => {
              setSearchTerm(term);
              setPage(0);
            }}
            placeholder="Search horse name, stall..."
            label="Search routine care"
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
              <Button
                variant="secondary"
                size="sm"
                icon="plus"
                onClick={() => setCreateModalOpen(true)}
                className="transition-all active:scale-[0.98]"
              >
                Schedule Routine Check
              </Button>
            }
          />
        </Panel>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {paginatedList.map((cs) => {
            const horse = horseMap.get(cs.horseId);
            const horseName = horse?.name || `Horse #${cs.horseId}`;
            const stallCode = horse?.currentStallId ? stallMap.get(horse.currentStallId) : 'Unassigned';
            const dateStr = cs.scheduledDate
              ? formatDate(cs.scheduledDate)
              : cs.scheduledAt
              ? formatDate(cs.scheduledAt)
              : 'Unscheduled';
            const tone = getCareTypeTone(cs.careType, cs.purpose, cs.sourceIncidentId);
            const isClosed = cs.status === 'COMPLETED';
            const rawDate = cs.scheduledDate || (cs.scheduledAt ? cs.scheduledAt.slice(0, 10) : null);
            const _d = new Date();
            const todayIso = `${_d.getFullYear()}-${String(_d.getMonth() + 1).padStart(2, '0')}-${String(_d.getDate()).padStart(2, '0')}`;
            const isFuture = Boolean(!isClosed && rawDate && rawDate > todayIso);
            // Window: ALL ROUTINE SCHEDULED exams (except PREVENTIVE) blocked outside 13:30–15:30, regardless of date
            const _minNow = _d.getHours() * 60 + _d.getMinutes();
            const isOutsideRoutineWindow =
              cs.careType === 'ROUTINE' &&
              cs.purpose !== 'PREVENTIVE' &&
              cs.status === 'SCHEDULED' &&
              (_minNow < 13 * 60 + 30 || _minNow > 15 * 60 + 30);

            const canExamine = cs.status === 'IN_PROGRESS' || (!isFuture && !isOutsideRoutineWindow);

            return (
              <Panel
                key={cs.id}
                padded
                onClick={() => {
                  if (isClosed || canExamine) {
                    handleOpenSchedule(cs);
                  }
                }}
                className={`border border-[var(--color-border)] transition-all duration-200 ease-out ${
                  isClosed || canExamine
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
                        {cs.status === 'COMPLETED' && (
                          <Pill tone="success" size="sm">
                            Completed
                          </Pill>
                        )}
                        {cs.status === 'IN_PROGRESS' && (
                          <Pill tone="info" size="sm" icon="activity">
                            In Progress
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
                          <Icon name="calendar" size={12} className="text-[var(--color-primary)]" />
                          <strong>{dateStr}</strong>
                        </span>
                        <span>·</span>
                        <span className="inline-flex items-center gap-1">
                          <Icon name="building" size={12} />
                          <span>Stall <strong>{stallCode}</strong></span>
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Right: Action Button */}
                  <div className="shrink-0 flex flex-col items-end gap-1">
                    <Button
                      variant={isClosed || cs.status === 'IN_PROGRESS' ? 'secondary' : 'primary'}
                      size="sm"
                      icon={isClosed ? 'file-text' : getScheduleExamAction(cs.status).icon}
                      disabled={!isClosed && !canExamine}
                      onClick={(e) => {
                        e.stopPropagation();
                        if (isClosed || canExamine) {
                          handleOpenSchedule(cs);
                        }
                      }}
                      className="transition-all active:scale-[0.97]"
                    >
                      {isClosed ? 'View Record' : getScheduleExamAction(cs.status).label}
                    </Button>
                    {!isClosed && isOutsideRoutineWindow && cs.status !== 'IN_PROGRESS' && (
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

          <Panel className="overflow-hidden">
            <Pagination
              page={currentPage}
              pageSize={PAGE_SIZE}
              total={filteredList.length}
              onPageChange={setPage}
              disabled={loading}
              className="border-t-0"
            />
          </Panel>
        </div>
      )}

      {/* Create Routine Modal */}
      <CreateRoutineModal
        open={createModalOpen}
        horses={horses}
        onClose={() => setCreateModalOpen(false)}
        onSuccess={loadData}
      />

    </div>
  );
}
