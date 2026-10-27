'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { FilterChips, SearchInput, Notice, PageHeader, MetricCard } from '@/components/ui';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import { stableApi } from '@/features/stable/services/stableService';
import type { CareSchedule, CareScheduleStatus } from '@/features/admissions/types';
import type { Horse, StableStall } from '@/features/stable/types';
import { CreateRoutineModal } from './CreateRoutineModal';
import { filterRoutineSchedules } from '../utils/routineValidation';

export function RoutineChecksWorkspace() {
  const router = useRouter();
  const [schedules, setSchedules] = useState<CareSchedule[]>([]);
  const [horses, setHorses] = useState<Horse[]>([]);
  const [stalls, setStalls] = useState<StableStall[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filters & Search
  const [statusFilter, setStatusFilter] = useState<CareScheduleStatus | 'ALL'>('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const [createModalOpen, setCreateModalOpen] = useState(false);

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
    router.push(`/veterinarian/routine/${item.id}`);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        eyebrow="Clinical Operations"
        title="Routine & Preventive Care"
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
              Schedule Care
            </Button>
          </div>
        }
      />

      {/* Metric Cards Banner */}
      <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <MetricCard
          label="Total Care Schedules"
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
          label="In Examination"
          value={stats.inProgress}
          icon="activity"
          tone={stats.inProgress > 0 ? 'info' : 'neutral'}
        />
        <MetricCard
          label="Completed Care"
          value={stats.completed}
          icon="check"
          tone={stats.completed > 0 ? 'success' : 'neutral'}
        />
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
          onChange={(v) => setStatusFilter(v as CareScheduleStatus | 'ALL')}
          options={[
            { value: 'ALL', label: 'All', count: schedules.length },
            { value: 'SCHEDULED', label: 'Scheduled', count: stats.scheduled },
            { value: 'IN_PROGRESS', label: 'In Progress', count: stats.inProgress },
            { value: 'COMPLETED', label: 'Completed', count: stats.completed },
          ]}
        />

        <div className="w-full sm:w-64">
          <SearchInput
            value={searchTerm}
            onChange={setSearchTerm}
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
                Schedule Care
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
              <Panel
                key={cs.id}
                padded
                className="flex flex-col justify-between border border-[var(--color-border)] transition-all duration-200 ease-out hover:-translate-y-0.5 hover:shadow-md hover:border-[var(--color-border-strong)]"
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
                            <Icon name="calendar" size={12} className="text-[var(--color-primary)]" />
                            <strong>{dateStr}</strong>
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
                    variant={cs.status === 'COMPLETED' ? 'secondary' : 'primary'}
                    size="sm"
                    icon={
                      cs.status === 'COMPLETED'
                        ? 'file-text'
                        : cs.status === 'IN_PROGRESS'
                        ? 'activity'
                        : 'chevron-right'
                    }
                    onClick={() => handleOpenSchedule(cs)}
                    className="transition-all active:scale-[0.97]"
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

    </div>
  );
}
