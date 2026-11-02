'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  EmptyState,
  IconButton,
  Input,
  ListSkeleton,
  PageHeader,
  Panel,
  ScreenLayout,
  Select,
  Tabs,
} from '@/components/ui';
import { useHorses } from '@/features/horses/hooks/useHorses';
import { displayError, formatDate } from '@/lib/display';
import { toast } from '@/lib/toast';
import { useTrainingRules } from '../hooks/useTrainingRules';
import { shiftIsoDate, toIsoDate } from '../lib/dateRange';
import { groupByDate, mondayOf, totalNeedingResult } from '../lib/groupSchedule';
import { trainingService } from '../services/trainingService';
import type { ScheduleSession, TrainingGroup } from '../types';
import { ScheduleWeekCalendar } from './ScheduleWeekCalendar';
import { SessionCard } from './SessionCard';
import { SessionDialog, SessionDetailContent } from './SessionDialog';
import { TimelineStrip } from './TimelineStrip';

type View = 'today' | 'week' | 'needs-result' | 'horse';

/** How far back the "Needs result" filter looks for Sessions whose results were never recorded. */
const NEEDS_RESULT_LOOKBACK_DAYS = 90;

const DAY_HEADING: Intl.DateTimeFormatOptions = { weekday: 'short', day: '2-digit', month: 'short' };

/**
 * The schedule of group Sessions:
 * - Today tab: Golden-hours interactive timeline strip with drag-to-reschedule, plus day cards.
 * - Week tab: 7-day grid showing all sessions of the yard across the week, filterable by group.
 * - Needs result tab: Sessions whose workout results were never recorded.
 * - Horse calendar tab: Inline 7-day calendar for each individual horse (not a popup).
 */
export function SchedulePage({ canManage }: { canManage: boolean }) {
  const today = toIsoDate(new Date());

  const [view, setView] = useState<View>('today');
  const [selectedDate, setSelectedDate] = useState(() => today);
  const [weekStart, setWeekStart] = useState(() => mondayOf(today));
  const [groupId, setGroupId] = useState<number | ''>('');
  const [missingCount, setMissingCount] = useState<number | null>(null);
  const [groups, setGroups] = useState<TrainingGroup[]>([]);
  const [sessions, setSessions] = useState<ScheduleSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);
  const [selectedHorseId, setSelectedHorseId] = useState<number | null>(null);

  const rules = useTrainingRules();
  const { horses } = useHorses({ mine: true });

  const loadMissingCount = useCallback(async () => {
    try {
      const items = await trainingService.getSessions({
        from: shiftIsoDate(today, -NEEDS_RESULT_LOOKBACK_DAYS),
        to: today,
        needsResult: true,
      });
      setMissingCount(totalNeedingResult(items));
    } catch {
      // ignore
    }
  }, [today]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadMissingCount();
  }, [loadMissingCount]);

  const activeHorse = useMemo(
    () => horses.find((h) => h.id === selectedHorseId) ?? horses[0] ?? null,
    [horses, selectedHorseId],
  );

  useEffect(() => {
    if (!canManage) return;
    let current = true;
    trainingService.getGroups().then((list) => current && setGroups(list)).catch(() => undefined);
    return () => {
      current = false;
    };
  }, [canManage]);

  const handleDateChange = useCallback(
    (newDate: string) => {
      if (newDate !== selectedDate) {
        setSessions([]);
        setLoading(true);
        setSelectedDate(newDate);
      }
    },
    [selectedDate],
  );

  const range = useMemo(() => {
    if (view === 'needs-result') {
      return { from: shiftIsoDate(today, -NEEDS_RESULT_LOOKBACK_DAYS), to: today };
    }
    return view === 'today'
      ? { from: selectedDate, to: selectedDate }
      : { from: weekStart, to: shiftIsoDate(weekStart, 6) };
  }, [view, weekStart, selectedDate, today]);

  const load = useCallback(async () => {
    if (view === 'horse') {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const loaded = await trainingService.getSessions({
        from: range.from,
        to: range.to,
        groupId: groupId === '' ? undefined : groupId,
        needsResult: view === 'needs-result',
      });
      setSessions(loaded);
      if (view === 'needs-result') {
        setMissingCount(totalNeedingResult(loaded));
      }
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the schedule.'));
    } finally {
      setLoading(false);
    }
  }, [range, groupId, view]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function moveSession(session: ScheduleSession, newStart: string) {
    try {
      await trainingService.rescheduleSession(session.id, `${newStart}:00`);
      toast.success(`${session.groupName} moved to ${newStart}.`);
      await Promise.all([load(), loadMissingCount()]);
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to move this session.'));
    }
  }

  const todaySessions = useMemo(() => {
    return sessions.filter((s) => s.status !== 'CANCELLED');
  }, [sessions]);

  const byDate = useMemo(() => groupByDate(sessions), [sessions]);

  return (
    <ScreenLayout variant="list">
      <PageHeader title="Schedule" />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Tabs
          tabs={[
            { id: 'today', label: 'Today', icon: 'clock' },
            { id: 'week', label: 'Week', icon: 'calendar' },
            { id: 'horse', label: 'Horse calendar', icon: 'horse' },
            {
              id: 'needs-result',
              label: `Needs result${missingCount !== null && missingCount > 0 ? ` (${missingCount})` : ''}`,
              icon: 'alert-triangle',
            },
          ]}
          active={view}
          onChange={(id) => {
            const next = id as View;
            if (next !== view) {
              setSessions([]);
              setLoading(true);
            }
            setView(next);
            if (next !== 'week') {
              setGroupId('');
            }
          }}
        />
        <div className="flex flex-wrap items-center gap-2">
          {canManage && (
            <>
              {view === 'horse' && horses.length > 0 && (
                <Select
                  aria-label="Select horse"
                  value={activeHorse?.id ?? ''}
                  onChange={(event) => {
                    const id = event.target.value ? Number(event.target.value) : null;
                    setSelectedHorseId(id);
                  }}
                  className="w-48 text-xs font-medium"
                >
                  {horses.map((h) => (
                    <option key={h.id} value={h.id}>{h.name}</option>
                  ))}
                </Select>
              )}
              {view === 'week' && (
                <Select
                  aria-label="Group"
                  value={groupId}
                  onChange={(event) => setGroupId(event.target.value ? Number(event.target.value) : '')}
                >
                  <option value="">All groups</option>
                  {groups.map((group) => (
                    <option key={group.id} value={group.id}>{group.name}</option>
                  ))}
                </Select>
              )}
            </>
          )}
        </div>
      </div>

      {view === 'horse' ? (
        activeHorse ? (
          <ScheduleWeekCalendar
            key={activeHorse.id}
            mode="horse"
            horseTarget={{ horseId: activeHorse.id, name: activeHorse.name, horse: activeHorse }}
            weekStart={weekStart}
            onWeekStartChange={setWeekStart}
            onOpenSession={setOpenId}
          />
        ) : (
          <Panel padded>
            <EmptyState icon="horse" title="No horses available" description="You do not have any horses assigned." />
          </Panel>
        )
      ) : loading ? (
        <Panel padded><ListSkeleton rows={4} /></Panel>
      ) : error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load the schedule"
            description={error}
            action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
          />
        </Panel>
      ) : view === 'needs-result' ? (
        <DayList byDate={byDate} onOpen={setOpenId} emptyTitle="Nothing is waiting for a result" />
      ) : view === 'today' ? (
        <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-3">
            <div className="flex items-center gap-2">
              <IconButton
                icon="chevron-left"
                label="Previous day"
                onClick={() => handleDateChange(shiftIsoDate(selectedDate, -1))}
              />
              <span className="min-w-44 text-center text-sm font-semibold text-[var(--color-text-primary)]">
                {formatDate(selectedDate, { weekday: 'short', day: '2-digit', month: 'short', year: 'numeric' })}
              </span>
              <IconButton
                icon="chevron-right"
                label="Next day"
                onClick={() => handleDateChange(shiftIsoDate(selectedDate, 1))}
              />
            </div>

            <div className="flex items-center gap-2">
              <Input
                type="date"
                value={selectedDate}
                onChange={(e) => e.target.value && handleDateChange(e.target.value)}
                className="w-auto text-xs py-1 h-8"
                aria-label="Pick a date"
              />
              <Button
                size="sm"
                variant="secondary"
                disabled={selectedDate === today}
                onClick={() => handleDateChange(today)}
              >
                Today
              </Button>
            </div>
          </div>

          {rules && (
            <TimelineStrip
              sessions={todaySessions}
              rules={rules}
              movable={canManage}
              onMove={moveSession}
              onOpen={setOpenId}
            />
          )}

          {todaySessions.length === 0 ? (
            <Panel padded>
              <EmptyState
                icon="calendar"
                title={`No sessions on ${formatDate(selectedDate, { weekday: 'short', day: '2-digit', month: 'short' })}`}
              />
            </Panel>
          ) : (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-[var(--color-text-secondary)]">
                {formatDate(selectedDate, DAY_HEADING)}
              </h2>
              <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                {todaySessions.map((session) => (
                  <Panel padded key={session.id}>
                    <SessionDetailContent
                      sessionId={session.id}
                      canManage={canManage}
                      onChanged={async () => {
                        await Promise.all([load(), loadMissingCount()]);
                      }}
                    />
                  </Panel>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <ScheduleWeekCalendar
          mode="group"
          groupId={groupId}
          groups={groups}
          sessions={sessions}
          loading={loading}
          weekStart={weekStart}
          onWeekStartChange={setWeekStart}
          onOpenSession={setOpenId}
        />
      )}

      <SessionDialog
        sessionId={openId}
        canManage={canManage}
        onClose={() => setOpenId(null)}
        onChanged={async () => {
          await Promise.all([load(), loadMissingCount()]);
        }}
      />
    </ScreenLayout>
  );
}

function DayList({
  byDate,
  onOpen,
  emptyTitle,
}: {
  byDate: Record<string, ScheduleSession[]>;
  onOpen: (id: number) => void;
  emptyTitle: string;
}) {
  const dates = Object.keys(byDate).sort();
  if (dates.length === 0) {
    return <Panel padded><EmptyState icon="calendar" title={emptyTitle} /></Panel>;
  }
  return (
    <div className="space-y-4">
      {dates.map((day) => (
        <section key={day} className="space-y-2">
          <h2 className="text-sm font-semibold text-[var(--color-text-secondary)]">{formatDate(day, DAY_HEADING)}</h2>
          <div className="grid grid-cols-1 gap-2 md:grid-cols-2 xl:grid-cols-3">
            {byDate[day].map((session) => (
              <SessionCard key={session.id} session={session} onOpen={onOpen} />
            ))}
          </div>
        </section>
      ))}
    </div>
  );
}
