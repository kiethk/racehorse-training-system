'use client';

import { useRef, useState } from 'react';
import { Panel } from '@/components/ui';
import { cn } from '@/lib/cn';
import { formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { clockOf, hourMarks, minutesOf, snapSessionStart, stripPosition } from '../../lib/timeline';
import type { SessionSummary, TrainingRules } from '../../types';

interface DragState {
  sessionId: number;
  pointerX: number;
  originMinutes: number;
  startMinutes: number;
  hasMoved: boolean;
}

function overlapsAnother(
  sessions: SessionSummary[],
  sessionId: number,
  startMinutes: number,
  duration: number,
): boolean {
  return sessions.some(
    (other) =>
      other.id !== sessionId &&
      other.status !== 'CANCELLED' &&
      startMinutes < minutesOf(other.endTime) &&
      startMinutes + duration > minutesOf(other.startTime),
  );
}

/**
 * The golden-hour window as a timeline strip, with every session drawn at its time.
 * A scheduled session can be dragged along the strip: it snaps to the 15-minute steps,
 * stays inside the window, and turns red over another session.
 */
export function TimelineStrip({
  sessions,
  rules,
  movable,
  onMove,
  onOpen,
}: {
  sessions: SessionSummary[];
  rules: TrainingRules;
  movable: boolean;
  onMove: (session: SessionSummary, newStart: string) => void;
  onOpen: (id: number) => void;
}) {
  const stripRef = useRef<HTMLDivElement>(null);
  const didDragRef = useRef(false);
  const [drag, setDrag] = useState<DragState | null>(null);

  const start = rules.goldenHoursStart || '06:00:00';
  const end = rules.goldenHoursEnd || '10:00:00';
  const step = rules.sessionStartStepMinutes || 15;
  const windowMinutes = minutesOf(end) - minutesOf(start);

  function canDrag(session: SessionSummary) {
    return movable && session.status === 'SCHEDULED' && !session.groupDisbanded;
  }

  function handlePointerDown(event: React.PointerEvent<HTMLDivElement>, session: SessionSummary) {
    if (!canDrag(session)) return;
    didDragRef.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
    const origin = minutesOf(session.startTime);
    setDrag({
      sessionId: session.id,
      pointerX: event.clientX,
      originMinutes: origin,
      startMinutes: origin,
      hasMoved: false,
    });
  }

  function handlePointerMove(event: React.PointerEvent<HTMLDivElement>, session: SessionSummary) {
    const width = stripRef.current?.clientWidth;
    if (!drag || drag.sessionId !== session.id || !width) return;
    const movedMinutes = ((event.clientX - drag.pointerX) / width) * windowMinutes;
    const duration = minutesOf(session.endTime) - minutesOf(session.startTime);
    const startMinutes = snapSessionStart(
      drag.originMinutes + movedMinutes,
      duration,
      step,
      start,
      end,
    );
    if (Math.abs(event.clientX - drag.pointerX) > 4) {
      didDragRef.current = true;
      if (startMinutes !== drag.startMinutes || !drag.hasMoved) {
        setDrag({ ...drag, startMinutes, hasMoved: true });
      }
    }
  }

  function handlePointerUp(session: SessionSummary) {
    if (!drag || drag.sessionId !== session.id) return;
    const hasMoved = drag.hasMoved;
    const targetStart = drag.startMinutes;
    const originStart = drag.originMinutes;
    setDrag(null);

    if (!hasMoved || targetStart === originStart) {
      didDragRef.current = false;
      return;
    }

    didDragRef.current = true;
    const duration = minutesOf(session.endTime) - minutesOf(session.startTime);
    if (overlapsAnother(sessions, session.id, targetStart, duration)) {
      toast.error('That time overlaps another session.');
      return;
    }
    onMove(session, clockOf(targetStart));
  }

  return (
    <Panel padded className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm font-semibold text-[var(--color-text-primary)]">
          Schedule window ({formatTime(start)} – {formatTime(end)})
        </p>
        <span className="text-xs text-[var(--color-text-muted)]">
          Drag scheduled sessions to reschedule · 15m step
        </span>
      </div>

      {/* Hour ruler */}
      <div
        className="relative h-5 border-b border-[var(--color-border)] text-xs text-[var(--color-text-muted)]"
        aria-hidden="true"
      >
        {hourMarks(start, end).map((mark) => (
          <span
            key={mark.label}
            className="absolute -translate-x-1/2 first:translate-x-0 last:-translate-x-full"
            style={{ left: `${mark.percent}%` }}
          >
            {mark.label}
          </span>
        ))}
      </div>

      {/* Strip track */}
      <div
        ref={stripRef}
        className="relative h-16 w-full overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)]"
      >
        {sessions.map((session) => {
          const isDragging = drag?.sessionId === session.id;
          const duration = minutesOf(session.endTime) - minutesOf(session.startTime);
          const currentStart = isDragging ? clockOf(drag.startMinutes) : session.startTime;
          const currentEnd = isDragging ? clockOf(drag.startMinutes + duration) : session.endTime;
          const { leftPercent, widthPercent } = stripPosition(currentStart, currentEnd, start, end);

          const isClash = isDragging && overlapsAnother(sessions, session.id, drag.startMinutes, duration);
          const isHeld = session.status === 'HELD';
          const isCancelled = session.status === 'CANCELLED';

          return (
            <div
              key={session.id}
              style={{ left: `${leftPercent}%`, width: `${widthPercent}%` }}
              onPointerDown={(e) => handlePointerDown(e, session)}
              onPointerMove={(e) => handlePointerMove(e, session)}
              onPointerUp={() => handlePointerUp(session)}
              onClick={() => {
                if (didDragRef.current) {
                  didDragRef.current = false;
                  return;
                }
                onOpen(session.id);
              }}
              className={cn(
                'absolute top-1 bottom-1 flex cursor-pointer select-none flex-col justify-center rounded-[var(--radius-sm)] border px-2 text-xs transition-shadow',
                canDrag(session) && 'cursor-grab active:cursor-grabbing',
                isClash
                  ? 'border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)] z-30'
                  : isDragging
                    ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)] shadow-lg z-20'
                    : isHeld
                      ? 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]'
                      : isCancelled
                        ? 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)] line-through opacity-60'
                        : 'border-[var(--color-primary)] bg-[var(--color-surface)] text-[var(--color-primary)] hover:border-[var(--color-primary-hover)]',
              )}
            >
              <p className="truncate font-semibold">{session.groupName}</p>
              <p className="truncate text-xs opacity-80">
                {formatTime(currentStart)}–{formatTime(currentEnd)} · {session.exerciseName}
              </p>
            </div>
          );
        })}
      </div>
    </Panel>
  );
}
