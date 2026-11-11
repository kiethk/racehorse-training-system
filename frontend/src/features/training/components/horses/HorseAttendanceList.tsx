'use client';

import { useEffect, useState } from 'react';
import { EmptyState, ListSkeleton, Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError, formatDate } from '@/lib/display';
import { attendanceLabel } from '../../lib/groupSchedule';
import { COURSE_RUN_STATUS } from '../../lib/status';
import { trainingService } from '../../services/trainingService';
import type { HorseAttendance } from '../../types';
import { StatusPill } from '../shared/StatusPill';
import { groupLabel } from '../../lib/groupLifecycle';

/**
 * Every Course Run a horse took part in, one line per course run, with how many Sessions it attended out of how many and since when.
 * A horse that changed groups shows a line for each, so its history is never lost.
 */
export function HorseAttendanceList({ horseId }: { horseId: number }) {
  const [history, setHistory] = useState<HorseAttendance[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let current = true;
    trainingService
      .getHorseAttendance(horseId)
      .then((data) => current && setHistory(data))
      .catch((cause) => current && setError(displayError(cause, 'Unable to load the training course runs.')));
    return () => {
      current = false;
    };
  }, [horseId]);

  if (error) return <Panel padded><EmptyState icon="alert-triangle" title="Unable to load" description={error} /></Panel>;
  if (!history) return <Panel padded><ListSkeleton rows={3} /></Panel>;
  if (history.length === 0) return <Panel padded><EmptyState icon="layers" title="No training course runs yet" /></Panel>;

  return (
    <div className="space-y-3">
      {history.map((line) => {
        const isCancelled = line.courseRunStatus === 'CANCELLED';
        return (
          <Panel
            key={line.courseRunId}
            padded
            className={cn(
              'flex flex-wrap items-center justify-between gap-3',
              isCancelled && 'bg-[var(--color-surface-subtle)] opacity-60'
            )}
          >
            <div className="min-w-0">
              <p
                className={cn(
                  'truncate text-sm font-semibold text-[var(--color-text-primary)]',
                  isCancelled && 'text-[var(--color-text-muted)] line-through'
                )}
              >
                {line.courseName}
              </p>
              <p className="truncate text-xs text-[var(--color-text-secondary)]">
                {groupLabel(line.groupName, line.groupDisbanded)} · joined {formatDate(line.joinedOn)}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <span
                className={cn(
                  'text-sm font-medium text-[var(--color-text-primary)]',
                  isCancelled && 'text-[var(--color-text-muted)]'
                )}
              >
                {attendanceLabel(line.attended, line.total)}
              </span>
              <StatusPill status={COURSE_RUN_STATUS[line.courseRunStatus]} />
            </div>
          </Panel>
        );
      })}
    </div>
  );
}
