'use client';

import { Pill } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatTime } from '@/lib/display';
import type { StatusStyle } from '../../lib/status';
import { StatusPill } from '../shared/StatusPill';

/** One Session in the week grid, shaped the same for a group's calendar and for a horse's calendar. */
export interface CommonCalendarItem {
  id: number;
  date: string;
  startTime: string;
  endTime: string;
  exerciseName: string;
  groupName?: string | null;
  groupDisbanded?: boolean;
  horseCount?: number;
  cancelled: boolean;
  /** The group Session's status, or in a horse's calendar its own Workout's (Needs result, Ran...). */
  pill: StatusStyle;
  needsResultCount?: number;
}

/** A small card for one Session in a day column. */
export function CalendarSessionCell({
  session,
  onOpen,
}: {
  session: CommonCalendarItem;
  onOpen?: (sessionId: number) => void;
}) {
  const isCancelled = session.cancelled;
  return (
  <div
    key={session.id}
    onClick={() => onOpen?.(session.id)}
    className={cn(
      'space-y-1 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2 shadow-xs transition',
      isCancelled && 'bg-[var(--color-surface-subtle)] opacity-60',
      onOpen &&
        'cursor-pointer hover:border-[var(--color-primary)] hover:shadow-sm',
    )}
  >
    <div className="flex items-center justify-between gap-1 text-xs">
      <span
        className={cn(
          'font-semibold text-[var(--color-text-primary)]',
          isCancelled && 'text-[var(--color-text-muted)] line-through',
        )}
      >
        {formatTime(session.startTime)}–{formatTime(session.endTime)}
      </span>
      <StatusPill status={session.pill} size="sm" />
    </div>

    {session.groupName && (
      <p
        className={cn(
          'truncate text-xs font-medium text-[var(--color-text-primary)]',
          isCancelled && 'text-[var(--color-text-muted)] line-through',
        )}
      >
        {session.groupName}
        {session.groupDisbanded && (
          <span className="ml-1 text-xs text-[var(--color-text-muted)]">(Disbanded)</span>
        )}
      </p>
    )}

    <p
      className={cn(
        'line-clamp-2 break-words text-xs text-[var(--color-text-secondary)]',
        isCancelled && 'text-[var(--color-text-muted)] line-through',
      )}
      title={session.exerciseName}
    >
      {session.exerciseName}
    </p>

    {(session.horseCount !== undefined ||
      (session.needsResultCount !== undefined && session.needsResultCount > 0)) && (
      <div className="flex items-center justify-between pt-0.5 text-xs text-[var(--color-text-muted)]">
        {session.horseCount !== undefined && (
          <span>
            {session.horseCount} {session.horseCount === 1 ? 'horse' : 'horses'}
          </span>
        )}
        {session.needsResultCount !== undefined && session.needsResultCount > 0 && (
          <Pill tone="warning" size="sm">
            Needs result
          </Pill>
        )}
      </div>
    )}
  </div>
  );
}
