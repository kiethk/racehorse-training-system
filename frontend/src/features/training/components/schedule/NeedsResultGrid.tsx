'use client';

import { EmptyState, Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatDate, formatTime } from '@/lib/display';
import { DAY_HEADING_FORMAT } from '../../lib/dateRange';
import { groupByDate, sessionTitle } from '../../lib/groupSchedule';
import { assignLanes, hourMarks, stripPosition } from '../../lib/timeline';
import type { SessionSummary, TrainingRules } from '../../types';

/** Width of the column with the days, in pixels. */
const DAY_WIDTH = 120;

/** Space between the edge of a Session and the hour line it starts or ends on, in pixels. */
const EDGE_GAP = 4;

/** Height of one row of Sessions inside a day, in pixels. */
const LANE_HEIGHT = 44;

/**
 * The Sessions that still wait for a result, as a time grid: the Golden Window across the top, one row per day down the side, and each
 * Session drawn at its time like an event of a calendar.
 */
export function NeedsResultGrid({
  sessions,
  rules,
  onOpen,
}: {
  sessions: SessionSummary[];
  rules: TrainingRules | null;
  onOpen: (id: number) => void;
}) {
  const byDate = groupByDate(sessions);
  const dates = Object.keys(byDate).sort();

  if (dates.length === 0) {
    return (
      <Panel padded>
        <EmptyState icon="calendar" title="Nothing is waiting for a result" />
      </Panel>
    );
  }

  const start = rules?.goldenHoursStart || '06:00:00';
  const end = rules?.goldenHoursEnd || '10:00:00';
  const marks = hourMarks(start, end);

  return (
    <Panel className="overflow-hidden">
      <div className="flex border-b border-[var(--color-border)] bg-[var(--color-surface-subtle)]">
        <div className="shrink-0" style={{ width: DAY_WIDTH }} />
        <div className="relative mx-5 h-8 flex-1 text-xs text-[var(--color-text-muted)]" aria-hidden="true">
          {marks.map((mark) => (
            <span
              key={mark.label}
              className="absolute top-1/2 -translate-x-1/2 -translate-y-1/2"
              style={{ left: `${mark.percent}%` }}
            >
              {mark.label}
            </span>
          ))}
        </div>
      </div>

      {dates.map((day) => {
        const daySessions = byDate[day];
        const { laneOf, laneCount } = assignLanes(daySessions);
        return (
          <div key={day} className="flex border-b border-[var(--color-border)] last:border-b-0">
            <div
              className="flex shrink-0 items-center px-2 text-xs font-semibold text-[var(--color-text-secondary)]"
              style={{ width: DAY_WIDTH }}
            >
              {formatDate(day, DAY_HEADING_FORMAT)}
            </div>
            <div className="relative mx-5 flex-1" style={{ height: laneCount * LANE_HEIGHT + 8 }}>
              {/* No line at the last hour: the edge of the panel already closes the window. */}
              {marks.slice(0, -1).map((mark) => (
                <div
                  key={mark.label}
                  aria-hidden="true"
                  className="absolute inset-y-0 border-l border-[var(--color-border)]"
                  style={{ left: `${mark.percent}%` }}
                />
              ))}
              {daySessions.map((session) => {
                const { leftPercent, widthPercent } = stripPosition(session.startTime, session.endTime, start, end);
                return (
                  <div
                    key={session.id}
                    role="button"
                    tabIndex={0}
                    style={{
                      left: `calc(${leftPercent}% + ${EDGE_GAP}px)`,
                      width: `calc(${widthPercent}% - ${EDGE_GAP * 2}px)`,
                      top: (laneOf.get(session.id) ?? 0) * LANE_HEIGHT + 4,
                      height: LANE_HEIGHT - 4,
                    }}
                    title={`${sessionTitle(session)} · ${formatTime(session.startTime)}–${formatTime(session.endTime)} · ${session.exerciseName} · ${session.needsResultCount} to record`}
                    onClick={() => onOpen(session.id)}
                    onKeyDown={(event) => {
                      if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        onOpen(session.id);
                      }
                    }}
                    className={cn(
                      'absolute flex cursor-pointer flex-col justify-center overflow-hidden rounded-[var(--radius-sm)] border px-2 text-xs',
                      'border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-text-primary)]',
                      'transition-opacity duration-[var(--duration-fast)] hover:opacity-80',
                      'focus-visible:outline-2 focus-visible:outline-[var(--color-focus)]',
                    )}
                  >
                    <p className="truncate font-semibold">{sessionTitle(session)}</p>
                    <p className="truncate text-[var(--color-text-secondary)]">
                      {formatTime(session.startTime)} · {session.needsResultCount}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        );
      })}
    </Panel>
  );
}
