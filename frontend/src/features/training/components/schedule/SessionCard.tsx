'use client';

import { Icon, Pill, SelectableCard } from '@/components/ui';
import { formatTime } from '@/lib/display';
import { sessionTitle } from '../../lib/groupSchedule';
import { SESSION_STATUS } from '../../lib/status';
import type { SessionSummary } from '../../types';
import { StatusPill } from '../shared/StatusPill';

/**
 * One Session as a card: time, group, exercise, how many horses and its status. A Held Session that still misses results carries a
 * "Needs result" label with the number of horses, so the Trainer notices what is left to record.
 */
export function SessionCard({ session, onOpen }: { session: SessionSummary; onOpen: (id: number) => void }) {
  return (
    <SelectableCard dimmed={session.status === 'CANCELLED'} onClick={() => onOpen(session.id)}>
      <div className="flex items-center justify-between gap-2">
        <span className="text-sm font-semibold text-[var(--color-text-primary)]">
          {formatTime(session.startTime)}–{formatTime(session.endTime)}
        </span>
        <StatusPill status={SESSION_STATUS[session.status]} />
      </div>
      <div className="mt-1 flex items-center gap-1.5 min-w-0">
        <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">{sessionTitle(session)}</p>
        {session.groupDisbanded && <Pill tone="neutral" size="sm">Disbanded</Pill>}
      </div>
      <p className="truncate text-xs text-[var(--color-text-secondary)]">{session.exerciseName}</p>
      <div className="mt-2 flex flex-wrap items-center gap-2 text-xs text-[var(--color-text-muted)]">
        <span className="flex items-center gap-1">
          <Icon name="horse" size={12} />
          {session.horseCount}
        </span>
        {session.needsResultCount > 0 && (
          <Pill tone="warning" size="sm">Needs result ({session.needsResultCount})</Pill>
        )}
      </div>
    </SelectableCard>
  );
}
