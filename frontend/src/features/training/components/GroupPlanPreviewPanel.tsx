'use client';

import { Button, Icon, Notice, Panel, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatDate, formatTime } from '@/lib/display';
import { PLAN_STATUS } from '../lib/status';
import type { GroupPlanPreview } from '../types';
import { StatusPill } from './StatusPill';

const DAY: Intl.DateTimeFormatOptions = { weekday: 'short', day: '2-digit', month: 'short' };

/**
 * What assigning the course would do, before anything is saved: the period, the time of every Session, the days with no free
 * slot and the plans that overlap. The backend decides all of it; this only shows it.
 */
export function GroupPlanPreviewPanel({
  preview,
  stale,
  onUseDate,
}: {
  preview: GroupPlanPreview;
  /** The preview belongs to the previous input and is being refreshed. */
  stale: boolean;
  onUseDate: (isoDate: string) => void;
}) {
  const full = new Set(preview.fullDates);

  return (
    <div className={cn('space-y-3 transition-opacity duration-[var(--duration-fast)]', stale && 'opacity-60')}>
      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] px-3 py-2 text-xs text-[var(--color-text-secondary)]">
        <span className="flex items-center gap-1.5 font-semibold text-[var(--color-text-primary)]">
          <Icon name="calendar" size={13} />
          {preview.sessions.length} sessions · {formatDate(preview.startDate, DAY)} → {formatDate(preview.endDate, DAY)}
        </span>
        <span className="flex items-center gap-1.5">
          <Icon name="horse" size={13} />
          {preview.memberCount} horses
        </span>
      </div>

      {preview.overlaps.map((overlap, index) => {
        const isLast = index === preview.overlaps.length - 1;
        return (
          <Notice key={overlap.planId} tone="warning">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <span className="flex flex-wrap items-center gap-2">
                Busy with {overlap.courseName} ({formatDate(overlap.startDate)} – {formatDate(overlap.endDate)})
                <StatusPill status={PLAN_STATUS[overlap.status]} />
              </span>
              {isLast && preview.earliestFreeStart && (
                <Button variant="link" size="sm" icon="chevron-right" onClick={() => onUseDate(preview.earliestFreeStart)}>
                  Start on {formatDate(preview.earliestFreeStart, DAY)}
                </Button>
              )}
            </div>
          </Notice>
        );
      })}
      {preview.fullDates.length > 0 && (
        <Notice tone="warning">
          Schedule full on {preview.fullDates.map((day) => formatDate(day, DAY)).join(', ')}
        </Notice>
      )}

      <Panel>
        <Table bare>
          <THead>
            <Tr>
              <Th>#</Th>
              <Th>Date</Th>
              <Th>Subject</Th>
              <Th>Time</Th>
            </Tr>
          </THead>
          <TBody>
            {preview.sessions.map((session, index) => (
              <Tr key={session.date}>
                <Td>{index + 1}</Td>
                <Td>{formatDate(session.date, DAY)}</Td>
                <Td>{session.subjectName}</Td>
                <Td className={cn(full.has(session.date) && 'text-[var(--color-danger)]')}>
                  {session.startTime && session.endTime
                    ? `${formatTime(session.startTime)} – ${formatTime(session.endTime)}`
                    : 'No free slot'}
                </Td>
              </Tr>
            ))}
          </TBody>
        </Table>
      </Panel>
    </div>
  );
}
