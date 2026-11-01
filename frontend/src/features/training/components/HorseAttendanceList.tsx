'use client';

import { useEffect, useState } from 'react';
import { EmptyState, ListSkeleton, Panel } from '@/components/ui';
import { displayError, formatDate } from '@/lib/display';
import { attendanceLabel } from '../lib/groupSchedule';
import { PLAN_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { HorseAttendance } from '../types';
import { StatusPill } from './StatusPill';
import { groupLabel } from '../lib/groupLifecycle';

/**
 * Every Group Plan a horse took part in, one line per plan, with how many Sessions it attended out of how many and since when.
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
      .catch((cause) => current && setError(displayError(cause, 'Unable to load the training history.')));
    return () => {
      current = false;
    };
  }, [horseId]);

  if (error) return <Panel padded><EmptyState icon="alert-triangle" title="Unable to load" description={error} /></Panel>;
  if (!history) return <Panel padded><ListSkeleton rows={3} /></Panel>;
  if (history.length === 0) return <Panel padded><EmptyState icon="layers" title="No training yet" /></Panel>;

  return (
    <div className="space-y-3">
      {history.map((line) => (
        <Panel key={line.planId} padded className="flex flex-wrap items-center justify-between gap-3">
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-[var(--color-text-primary)]">{line.courseName}</p>
            <p className="truncate text-xs text-[var(--color-text-secondary)]">
              {groupLabel(line.groupName, line.groupDisbanded)} · joined {formatDate(line.joinedOn)}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="text-sm font-medium text-[var(--color-text-primary)]">
              {attendanceLabel(line.attended, line.total)}
            </span>
            <StatusPill status={PLAN_STATUS[line.planStatus]} />
          </div>
        </Panel>
      ))}
    </div>
  );
}
