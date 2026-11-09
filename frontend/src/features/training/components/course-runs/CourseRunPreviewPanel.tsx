'use client';

import { DAY_HEADING_FORMAT } from '../../lib/dateRange';
import { Button, Icon, Notice, Panel, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatDate, formatTime } from '@/lib/display';
import { COURSE_RUN_STATUS } from '../../lib/status';
import type { CourseRunPreview } from '../../types';
import { StatusPill } from '../shared/StatusPill';

/**
 * What assigning the course would do, before anything is saved: the period, the time of every Session, the days with no free
 * slot and the course runs that overlap. The backend decides all of it; this only shows it.
 */
export function CourseRunPreviewPanel({
  preview,
  stale,
  onUseDate,
}: {
  preview: CourseRunPreview;
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
          {preview.sessions.length} sessions · {formatDate(preview.startDate, DAY_HEADING_FORMAT)} → {formatDate(preview.endDate, DAY_HEADING_FORMAT)}
        </span>
        <span className="flex items-center gap-1.5">
          <Icon name="horse" size={13} />
          {preview.memberCount} horses
        </span>
      </div>

      {preview.overlaps.map((overlap, index) => {
        const isLast = index === preview.overlaps.length - 1;
        return (
          <Notice key={overlap.courseRunId} tone="warning">
            <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
              <span className="flex flex-wrap items-center gap-2">
                Busy with {overlap.courseName} ({formatDate(overlap.startDate)} – {formatDate(overlap.endDate)})
                <StatusPill status={COURSE_RUN_STATUS[overlap.status]} />
              </span>
              {isLast && preview.earliestFreeStart && (
                <Button variant="link" size="sm" icon="chevron-right" onClick={() => onUseDate(preview.earliestFreeStart)}>
                  Start on {formatDate(preview.earliestFreeStart, DAY_HEADING_FORMAT)}
                </Button>
              )}
            </div>
          </Notice>
        );
      })}
      {preview.fullDates.length > 0 && (
        <Notice tone="warning">
          Schedule full on {preview.fullDates.map((day) => formatDate(day, DAY_HEADING_FORMAT)).join(', ')}
        </Notice>
      )}

      <Panel>
        <Table bare>
          <THead>
            <Tr>
              <Th>#</Th>
              <Th>Date</Th>
              <Th>Exercise</Th>
              <Th>Time</Th>
            </Tr>
          </THead>
          <TBody>
            {preview.sessions.map((session, index) => (
              <Tr key={session.date}>
                <Td>{index + 1}</Td>
                <Td>{formatDate(session.date, DAY_HEADING_FORMAT)}</Td>
                <Td>{session.exerciseName}</Td>
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
