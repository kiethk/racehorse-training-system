'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  EmptyState,
  IconButton,
  ListSkeleton,
  PageHeader,
  Panel,
  ScreenLayout,
  Select,
  Tabs,
} from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError, formatDate } from '@/lib/display';
import { shiftIsoDate, toIsoDate } from '../lib/dateRange';
import { groupByDate, mondayOf, totalNeedingResult, weekDates } from '../lib/groupSchedule';
import { trainingService } from '../services/trainingService';
import type { ScheduleSession, TrainingGroup } from '../types';
import { SessionCard } from './SessionCard';
import { SessionDialog } from './SessionDialog';

type View = 'today' | 'week';

/** How far back the "Needs result" filter looks for Sessions whose results were never recorded. */
const NEEDS_RESULT_LOOKBACK_DAYS = 90;

const DAY_HEADING: Intl.DateTimeFormatOptions = { weekday: 'short', day: '2-digit', month: 'short' };

/**
 * The schedule of group Sessions, with Today and Week tabs sharing one query by date range. Trainers can filter by group, see which
 * Held Sessions still miss results, and change Sessions; Grooms and Owners see only their own horses and cannot change anything.
 */
export function SchedulePage({ canManage }: { canManage: boolean }) {
  const today = toIsoDate(new Date());

  const [view, setView] = useState<View>('today');
  const [weekStart, setWeekStart] = useState(() => mondayOf(today));
  const [groupId, setGroupId] = useState<number | ''>('');
  const [needsResult, setNeedsResult] = useState(false);
  const [groups, setGroups] = useState<TrainingGroup[]>([]);
  const [sessions, setSessions] = useState<ScheduleSession[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [openId, setOpenId] = useState<number | null>(null);

  useEffect(() => {
    if (!canManage) return;
    let current = true;
    trainingService.getGroups().then((list) => current && setGroups(list)).catch(() => undefined);
    return () => {
      current = false;
    };
  }, [canManage]);

  const range = useMemo(() => {
    if (needsResult) return { from: shiftIsoDate(today, -NEEDS_RESULT_LOOKBACK_DAYS), to: today };
    return view === 'today' ? { from: today, to: today } : { from: weekStart, to: shiftIsoDate(weekStart, 6) };
  }, [needsResult, view, weekStart, today]);

  const load = useCallback(async () => {
    setError(null);
    try {
      setSessions(await trainingService.getSessions({
        from: range.from,
        to: range.to,
        groupId: groupId === '' ? undefined : groupId,
        needsResult,
      }));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the schedule.'));
    } finally {
      setLoading(false);
    }
  }, [range, groupId, needsResult]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const byDate = useMemo(() => groupByDate(sessions), [sessions]);
  const missing = totalNeedingResult(sessions);

  return (
    <ScreenLayout variant="list">
      <PageHeader title="Schedule" />

      <div className="flex flex-wrap items-center justify-between gap-3">
        <Tabs
          tabs={[
            { id: 'today', label: 'Today', icon: 'clock' },
            { id: 'week', label: 'Week', icon: 'calendar' },
          ]}
          active={view}
          onChange={(id) => setView(id as View)}
        />
        <div className="flex flex-wrap items-center gap-2">
          {canManage && (
            <>
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
              <Button
                size="sm"
                variant={needsResult ? 'primary' : 'secondary'}
                aria-pressed={needsResult}
                onClick={() => setNeedsResult((value) => !value)}
              >
                Needs result{needsResult && missing > 0 ? ` (${missing})` : ''}
              </Button>
            </>
          )}
        </div>
      </div>

      {view === 'week' && !needsResult && (
        <div className="flex items-center gap-2">
          <IconButton icon="chevron-left" label="Previous week" onClick={() => setWeekStart((value) => shiftIsoDate(value, -7))} />
          <span className="min-w-48 text-center text-sm font-medium text-[var(--color-text-primary)]">
            {formatDate(weekStart, { day: '2-digit', month: 'short' })} – {formatDate(shiftIsoDate(weekStart, 6), { day: '2-digit', month: 'short', year: 'numeric' })}
          </span>
          <IconButton icon="chevron-right" label="Next week" onClick={() => setWeekStart((value) => shiftIsoDate(value, 7))} />
          <Button size="sm" onClick={() => setWeekStart(mondayOf(today))}>This week</Button>
        </div>
      )}

      {loading ? (
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
      ) : needsResult || view === 'today' ? (
        <DayList byDate={byDate} onOpen={setOpenId} emptyTitle={needsResult ? 'Nothing is waiting for a result' : 'No sessions today'} />
      ) : (
        <WeekGrid byDate={byDate} days={weekDates(weekStart)} today={today} onOpen={setOpenId} />
      )}

      <SessionDialog
        sessionId={openId}
        canManage={canManage}
        onClose={() => setOpenId(null)}
        onChanged={load}
      />
    </ScreenLayout>
  );
}

function WeekGrid({
  byDate,
  days,
  today,
  onOpen,
}: {
  byDate: Record<string, ScheduleSession[]>;
  days: string[];
  today: string;
  onOpen: (id: number) => void;
}) {
  return (
    <div className="grid grid-cols-1 gap-3 md:grid-cols-4 xl:grid-cols-7">
      {days.map((day) => (
        <div key={day} className="space-y-2">
          <p
            className={cn(
              'rounded-[var(--radius-sm)] px-2 py-1 text-xs font-semibold',
              day === today
                ? 'bg-[var(--color-primary-soft)] text-[var(--color-primary)]'
                : 'bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]',
            )}
          >
            {formatDate(day, DAY_HEADING)}
          </p>
          {(byDate[day] ?? []).map((session) => (
            <SessionCard key={session.id} session={session} onOpen={onOpen} />
          ))}
          {!byDate[day] && <p className="px-2 text-xs text-[var(--color-text-muted)]">—</p>}
        </div>
      ))}
    </div>
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
