'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Button,
  HorseAvatar,
  Icon,
  IconButton,
  LinkButton,
  Notice,
  Panel,
  Pill,
  Skeleton,
  StatusBadge,
} from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError, formatDate, formatEnumLabel } from '@/lib/display';
import type { Horse } from '@/features/stable/types';
import { shiftIsoDate, toIsoDate } from '../../lib/dateRange';
import { mondayOf } from '../../lib/groupSchedule';
import { scheduleDates, SCHEDULE_DAYS } from '../../lib/horseSchedule';
import { SESSION_STATUS, WORKOUT_STATUS } from '../../lib/status';
import { trainingService } from '../../services/trainingService';
import type { HorseScheduleItem, SessionSummary, TrainingGroup } from '../../types';
import { CalendarSessionCell, type CommonCalendarItem } from './CalendarSessionCell';

export interface ScheduleWeekCalendarProps {
  mode: 'group' | 'horse';
  weekStart: string;
  onWeekStartChange?: (newWeekStart: string) => void;
  // Horse mode target:
  horseTarget?: { horseId: number; name: string; horse?: Horse } | null;
  // Group mode filter & metadata:
  groupId?: number | '';
  groups?: TrainingGroup[];
  // If pre-loaded sessions are passed from page:
  sessions?: SessionSummary[];
  loading?: boolean;
  onOpenSession?: (sessionId: number) => void;
  hideHeader?: boolean;
}

function dayHeading(isoDate: string) {
  const [year, month, day] = isoDate.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return {
    weekday: formatDate(date, { weekday: 'short' }),
    day: formatDate(date, { day: '2-digit', month: '2-digit' }),
  };
}

/**
 * Shared week calendar component for both Group and Horse schedules.
 * Provides unified panel wrapping, header banner, week navigation, and 7-day column grid.
 */
export function ScheduleWeekCalendar({
  mode,
  weekStart: externalWeekStart,
  onWeekStartChange,
  horseTarget,
  groupId,
  groups = [],
  sessions: preloadedSessions,
  loading: externalLoading,
  onOpenSession,
  hideHeader = false,
}: ScheduleWeekCalendarProps) {
  const [internalWeekStart, setInternalWeekStart] = useState(externalWeekStart);
  const currentWeekStart = onWeekStartChange ? externalWeekStart : internalWeekStart;

  const setWeekStart = useCallback(
    (newDate: string) => {
      if (onWeekStartChange) {
        onWeekStartChange(newDate);
      } else {
        setInternalWeekStart(newDate);
      }
    },
    [onWeekStartChange],
  );

  // Self-fetching for horse mode or when sessions not passed
  const [horseItems, setHorseItems] = useState<HorseScheduleItem[]>([]);
  const [groupSessions, setGroupSessions] = useState<SessionSummary[]>([]);
  const [internalLoading, setInternalLoading] = useState(false);
  const [fetchError, setFetchError] = useState<string | null>(null);

  const shouldFetchSelf = mode === 'horse' || preloadedSessions === undefined;

  useEffect(() => {
    if (!shouldFetchSelf) return;

    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setInternalLoading(true);
    setFetchError(null);

    const weekEnd = shiftIsoDate(currentWeekStart, SCHEDULE_DAYS - 1);

    if (mode === 'horse' && horseTarget) {
      trainingService
        .getHorseSchedule(horseTarget.horseId, currentWeekStart, weekEnd)
        .then((result) => active && setHorseItems(result))
        .catch((cause) => active && setFetchError(displayError(cause, 'Unable to load horse schedule.')))
        .finally(() => active && setInternalLoading(false));
    } else if (mode === 'group') {
      trainingService
        .getSessions({
          from: currentWeekStart,
          to: weekEnd,
          groupId: groupId === '' ? undefined : groupId,
        })
        .then((result) => active && setGroupSessions(result))
        .catch((cause) => active && setFetchError(displayError(cause, 'Unable to load schedule.')))
        .finally(() => active && setInternalLoading(false));
    } else {
      setInternalLoading(false);
    }

    return () => {
      active = false;
    };
  }, [mode, horseTarget, currentWeekStart, shouldFetchSelf, groupId]);

  const loading = externalLoading ?? internalLoading;
  const today = toIsoDate(new Date());
  const dates = useMemo(() => scheduleDates(currentWeekStart), [currentWeekStart]);

  // Normalize items to CommonCalendarItem
  const itemsByDate = useMemo(() => {
    const map = new Map<string, CommonCalendarItem[]>();
    for (const d of dates) {
      map.set(d, []);
    }

    if (mode === 'horse') {
      for (const item of horseItems) {
        const list = map.get(item.sessionDate) ?? [];
        const cancelled = item.sessionStatus === 'CANCELLED' || item.workoutStatus === 'CANCELLED';

        list.push({
          id: item.sessionId,
          date: item.sessionDate,
          startTime: item.startTime,
          endTime: item.endTime,
          exerciseName: item.exerciseName,
          cancelled,
          pill: WORKOUT_STATUS[cancelled ? 'CANCELLED' : item.workoutStatus],
        });
        map.set(item.sessionDate, list);
      }
    } else {
      const source = preloadedSessions ?? groupSessions;
      const filtered = preloadedSessions === undefined && groupId ? source.filter((s) => s.groupId === groupId) : source;
      for (const s of filtered) {
        const list = map.get(s.date) ?? [];
        list.push({
          id: s.id,
          date: s.date,
          startTime: s.startTime,
          endTime: s.endTime,
          exerciseName: s.exerciseName,
          groupName: s.groupName,
          groupDisbanded: s.groupDisbanded,
          horseCount: s.horseCount,
          cancelled: s.status === 'CANCELLED',
          pill: SESSION_STATUS[s.status],
          needsResultCount: s.needsResultCount,
        });
        map.set(s.date, list);
      }
    }

    return map;
  }, [mode, horseItems, preloadedSessions, groupSessions, groupId, dates]);

  const rangeLabel = `${formatDate(dates[0], { day: '2-digit', month: '2-digit' })} – ${formatDate(
    dates[SCHEDULE_DAYS - 1],
    { day: '2-digit', month: '2-digit', year: 'numeric' },
  )}`;
  const isCurrentWeek = currentWeekStart === mondayOf(today);

  // Header info for horse mode
  const horse = horseTarget?.horse;
  const horseAbout = horse
    ? [
        horse.breed,
        horse.sex && formatEnumLabel(horse.sex),
        horse.dateOfBirth && `Born ${formatDate(horse.dateOfBirth)}`,
        horse.registrationNumber && `Reg. ${horse.registrationNumber}`,
      ]
        .filter(Boolean)
        .join(' · ')
    : null;

  // Header info for group mode
  const selectedGroup = useMemo(() => {
    if (!groupId) return null;
    return groups.find((g) => g.id === groupId) ?? null;
  }, [groupId, groups]);

  return (
    <Panel padded className="space-y-4">
      {!hideHeader && (
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-[var(--color-border)] pb-3">
          {mode === 'horse' && horseTarget ? (
            <>
              <div className="flex min-w-0 items-center gap-3">
                <HorseAvatar name={horseTarget.name} image={horse?.imageUrl} size={44} rounded="md" />
                <div className="min-w-0 space-y-0.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-sm text-[var(--color-text-primary)]">{horseTarget.name}</span>
                    {horse && <StatusBadge status={horse.currentStatus} />}
                    {horse?.trainingDecision === 'BLOCKED' && (
                      <Pill tone="danger" icon="lock" size="sm">
                        Training blocked
                      </Pill>
                    )}
                  </div>
                  {horseAbout && <p className="text-xs text-[var(--color-text-secondary)]">{horseAbout}</p>}
                </div>
              </div>
              <LinkButton href={`/trainer/horses/${horseTarget.horseId}`} icon="horse" size="sm">
                Open profile
              </LinkButton>
            </>
          ) : mode === 'group' && selectedGroup ? (
            <>
              <div className="flex min-w-0 items-center gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-md bg-[var(--color-primary-soft)] text-sm font-semibold text-[var(--color-primary)]">
                  {selectedGroup.name.slice(0, 2).toUpperCase()}
                </div>
                <div className="min-w-0 space-y-0.5">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-sm text-[var(--color-text-primary)]">
                      {selectedGroup.name}
                    </span>
                    <Pill tone={selectedGroup.disbanded ? 'neutral' : 'success'} size="sm">
                      {selectedGroup.disbanded ? 'Disbanded' : 'Active group'}
                    </Pill>
                  </div>
                  <p className="text-xs text-[var(--color-text-secondary)]">
                    {selectedGroup.members.length} {selectedGroup.members.length === 1 ? 'horse' : 'horses'} assigned
                  </p>
                </div>
              </div>
              <LinkButton href="/trainer/groups" icon="users" size="sm">
                Manage groups
              </LinkButton>
            </>
          ) : (
            <>
              <div className="flex min-w-0 items-center gap-2.5">
                <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
                  <Icon name="calendar" size={18} />
                </div>
                <span className="font-semibold text-sm text-[var(--color-text-primary)]">
                  All Training Groups
                </span>
              </div>
              <LinkButton href="/trainer/groups" icon="users" size="sm">
                Manage groups
              </LinkButton>
            </>
          )}
        </div>
      )}

      {/* Centered navigation matching horse calendar */}
      <div className="flex items-center justify-between gap-2">
        <IconButton
          icon="chevron-left"
          label="Previous 7 days"
          onClick={() => setWeekStart(shiftIsoDate(currentWeekStart, -SCHEDULE_DAYS))}
        />
        <div className="flex items-center gap-2">
          <p className="text-sm font-semibold text-[var(--color-text-primary)]">{rangeLabel}</p>
          {!isCurrentWeek && (
            <Button size="sm" variant="secondary" onClick={() => setWeekStart(mondayOf(today))}>
              This week
            </Button>
          )}
        </div>
        <IconButton
          icon="chevron-right"
          label="Next 7 days"
          onClick={() => setWeekStart(shiftIsoDate(currentWeekStart, SCHEDULE_DAYS))}
        />
      </div>

      {fetchError && <Notice tone="error">{fetchError}</Notice>}

      {/* 7-day column grid */}
      <div className="grid grid-cols-1 gap-2 md:grid-cols-4 xl:grid-cols-7">
        {dates.map((date) => {
          const heading = dayHeading(date);
          const daySessions = itemsByDate.get(date) ?? [];
          const isToday = date === today;

          return (
            <div
              key={date}
              className={cn(
                'min-w-0 space-y-2 rounded-[var(--radius-md)] border p-2',
                isToday
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]/20'
                  : 'border-[var(--color-border)] bg-[var(--color-surface-subtle)]',
              )}
            >
              <div className="border-b border-[var(--color-border)] pb-1.5 text-center">
                <p
                  className={cn(
                    'text-xs font-semibold',
                    isToday ? 'text-[var(--color-primary)]' : 'text-[var(--color-text-primary)]',
                  )}
                >
                  {heading.weekday}
                </p>
                <p className="text-xs text-[var(--color-text-muted)]">{heading.day}</p>
              </div>

              {loading ? (
                <Skeleton className="h-16 w-full" />
              ) : daySessions.length === 0 ? (
                <p className="py-4 text-center text-xs text-[var(--color-text-muted)]">—</p>
              ) : (
                <div className="space-y-1.5">
                  {daySessions.map((session) => (
                    <CalendarSessionCell key={session.id} session={session} onOpen={onOpenSession} />
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </Panel>
  );
}
