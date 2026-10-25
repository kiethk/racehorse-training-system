'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Button,
  ConfirmDialog,
  EmptyState,
  IconButton,
  Input,
  ListSkeleton,
  Notice,
  PageHeader,
  PageSection,
  Panel,
  Pill,
  ScreenLayout,
} from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError, formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { useTrainingRules } from '../hooks/useTrainingRules';
import { shiftIsoDate, toIsoDate } from '../lib/dateRange';
import { lotFillTone } from '../lib/status';
import { clockOf, hourMarks, minutesOf, snapLotStart, stripPosition } from '../lib/timeline';
import { trainingService } from '../services/trainingService';
import type { TrainingLotResponse, TrainingRules } from '../types';
import { RescheduleDialog } from './RescheduleDialog';

const STRIP_TONE = {
  danger: 'border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]',
  warning: 'border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-warning)]',
  success: 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]',
} as const;

/** A lot being dragged: where the pointer started, and the start time it would get if dropped now. */
interface DragState {
  lotId: number;
  pointerX: number;
  originMinutes: number;
  startMinutes: number;
}

function overlapsAnother(lots: TrainingLotResponse[], lotId: number, startMinutes: number, duration: number): boolean {
  return lots.some(
    (other) =>
      other.lotId !== lotId &&
      startMinutes < minutesOf(other.endTime) &&
      startMinutes + duration > minutesOf(other.startTime),
  );
}

/**
 * The golden-hour window as a strip, with every lot drawn at its time. A scheduled lot can be dragged along the
 * strip: it snaps to the quarter-hour steps, stays inside the window, and turns red over another lot (dropping it
 * there puts it back). The Reschedule button on each card does the same from the keyboard.
 */
function TimelineStrip({
  lots,
  rules,
  movable,
  onMove,
}: {
  lots: TrainingLotResponse[];
  rules: TrainingRules;
  /** False for past days: their lots have already happened. */
  movable: boolean;
  onMove: (lot: TrainingLotResponse, newStart: string) => void;
}) {
  const stripRef = useRef<HTMLDivElement>(null);
  const [drag, setDrag] = useState<DragState | null>(null);
  const start = rules.goldenHoursStart;
  const end = rules.goldenHoursEnd;
  const windowMinutes = minutesOf(end) - minutesOf(start);

  function canDrag(lot: TrainingLotResponse) {
    return movable && lot.status === 'SCHEDULED';
  }

  function handlePointerDown(event: React.PointerEvent<HTMLDivElement>, lot: TrainingLotResponse) {
    if (!canDrag(lot)) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    const origin = minutesOf(lot.startTime);
    setDrag({ lotId: lot.lotId, pointerX: event.clientX, originMinutes: origin, startMinutes: origin });
  }

  function handlePointerMove(event: React.PointerEvent<HTMLDivElement>, lot: TrainingLotResponse) {
    const width = stripRef.current?.clientWidth;
    if (!drag || drag.lotId !== lot.lotId || !width) return;
    const movedMinutes = ((event.clientX - drag.pointerX) / width) * windowMinutes;
    const startMinutes = snapLotStart(
      drag.originMinutes + movedMinutes, lot.durationMinutes, rules.lotStartStepMinutes, start, end,
    );
    if (startMinutes !== drag.startMinutes) setDrag({ ...drag, startMinutes });
  }

  function handlePointerUp(lot: TrainingLotResponse) {
    if (!drag || drag.lotId !== lot.lotId) return;
    setDrag(null);
    if (drag.startMinutes === drag.originMinutes) return;
    if (overlapsAnother(lots, lot.lotId, drag.startMinutes, lot.durationMinutes)) {
      toast.error('That time overlaps another lot.');
      return;
    }
    onMove(lot, clockOf(drag.startMinutes));
  }

  return (
    <Panel padded className="space-y-3">
      <p className="text-sm font-semibold text-[var(--color-text-primary)]">
        Schedule window {formatTime(start)}–{formatTime(end)}
      </p>

      <div className="relative h-5 border-b border-[var(--color-border)] text-xs text-[var(--color-text-muted)]" aria-hidden="true">
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

      <div
        ref={stripRef}
        className="relative h-16 w-full overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)]"
      >
        {lots.map((lot) => {
          const dragging = drag?.lotId === lot.lotId;
          const startMinutes = dragging ? drag.startMinutes : minutesOf(lot.startTime);
          const endMinutes = startMinutes + lot.durationMinutes;
          const clash = dragging && overlapsAnother(lots, lot.lotId, startMinutes, lot.durationMinutes);
          const { leftPercent, widthPercent } = stripPosition(clockOf(startMinutes), clockOf(endMinutes), start, end);
          const draggable = canDrag(lot);
          return (
            <div
              key={lot.lotId}
              style={{ left: `${leftPercent}%`, width: `${widthPercent}%` }}
              className={cn(
                'absolute bottom-1 top-1 flex touch-none select-none flex-col justify-center overflow-hidden rounded-[var(--radius-sm)] border px-2',
                clash ? STRIP_TONE.danger : STRIP_TONE[lotFillTone(lot.remainingSlots)],
                draggable && 'cursor-grab',
                dragging && 'z-10 cursor-grabbing shadow-[var(--shadow-popover)]',
              )}
              title={draggable ? 'Drag to change the time' : undefined}
              onPointerDown={(event) => handlePointerDown(event, lot)}
              onPointerMove={(event) => handlePointerMove(event, lot)}
              onPointerUp={() => handlePointerUp(lot)}
              onPointerCancel={() => setDrag(null)}
            >
              <span className="truncate text-xs font-semibold">{lot.subjectName}</span>
              <span className="truncate text-xs">
                {clockOf(startMinutes)}–{clockOf(endMinutes)} ({lot.occupied}/{lot.maxCapacity})
              </span>
            </div>
          );
        })}
      </div>
    </Panel>
  );
}

function LotCard({
  lot,
  onReschedule,
  onCancel,
}: {
  lot: TrainingLotResponse;
  onReschedule: () => void;
  onCancel: () => void;
}) {
  return (
    <Panel padded className="flex flex-col justify-between gap-4">
      <div className="space-y-3">
        <div className="flex items-start justify-between gap-2">
          <div>
            <h3 className="text-base font-semibold text-[var(--color-text-primary)]">{lot.subjectName}</h3>
            <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">
              <strong>{formatTime(lot.startTime)} – {formatTime(lot.endTime)}</strong> ({lot.durationMinutes} min)
            </p>
          </div>
          <Pill tone={lotFillTone(lot.remainingSlots)} size="sm">
            {lot.occupied}/{lot.maxCapacity} horses{lot.remainingSlots === 0 ? ' (full)' : ''}
          </Pill>
        </div>

        <div className="space-y-1.5 border-t border-[var(--color-border)] pt-2">
          <p className="text-xs text-[var(--color-text-muted)]">Participating horses ({lot.horseNames.length})</p>
          {lot.horseNames.length === 0 ? (
            <p className="text-xs text-[var(--color-text-muted)]">No horses</p>
          ) : (
            <ul className="flex flex-wrap gap-1.5">
              {lot.horseNames.map((name) => (
                <li key={name}><Pill tone="neutral" size="sm" icon="horse">{name}</Pill></li>
              ))}
            </ul>
          )}
        </div>
      </div>

      <div className="flex justify-end gap-2 border-t border-[var(--color-border)] pt-3">
        <Button size="sm" onClick={onReschedule}>Reschedule</Button>
        <Button size="sm" variant="destructive" onClick={onCancel}>Cancel lot</Button>
      </div>
    </Panel>
  );
}

export function LotTimeline() {
  const rules = useTrainingRules();

  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [lots, setLots] = useState<TrainingLotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [rescheduleTarget, setRescheduleTarget] = useState<TrainingLotResponse | null>(null);
  const [cancelTarget, setCancelTarget] = useState<TrainingLotResponse | null>(null);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setLots(await trainingService.getLots(date, date, false));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load training lots for the selected date.'));
    } finally {
      setLoading(false);
    }
  }, [date]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const sortedLots = useMemo(() => [...lots].sort((a, b) => a.startTime.localeCompare(b.startTime)), [lots]);
  const today = toIsoDate(new Date());

  /** Drop on the timeline: the lot is shown at its new time at once, and put back if the backend refuses. */
  async function moveLot(lot: TrainingLotResponse, newStart: string) {
    const newEnd = clockOf(minutesOf(newStart) + lot.durationMinutes);
    setLots((current) =>
      current.map((l) => (l.lotId === lot.lotId ? { ...l, startTime: `${newStart}:00`, endTime: `${newEnd}:00` } : l)),
    );
    try {
      await trainingService.rescheduleLot(lot.lotId, { newStartTime: `${newStart}:00` });
      toast.success(`${lot.subjectName} moved to ${newStart}.`);
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to move this lot.'));
    }
    await load();
  }

  async function confirmCancel() {
    if (!cancelTarget) return;
    setCancelling(true);
    try {
      await trainingService.cancelLot(cancelTarget.lotId);
      toast.success(`${cancelTarget.subjectName} cancelled.`);
      setCancelTarget(null);
      await load();
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to cancel this lot.'));
    } finally {
      setCancelling(false);
    }
  }

  return (
    <ScreenLayout variant="list">
      <PageHeader
        title="Daily training schedule"
        actions={
          <div className="flex items-center gap-1">
            <IconButton icon="chevron-left" label="Previous day" onClick={() => setDate((d) => shiftIsoDate(d, -1))} />
            <Input
              type="date"
              aria-label="Date"
              className="w-40"
              value={date}
              onChange={(e) => e.target.value && setDate(e.target.value)}
            />
            <IconButton icon="chevron-right" label="Next day" onClick={() => setDate((d) => shiftIsoDate(d, 1))} />
          </div>
        }
      />

      {error ? (
        <Panel padded>
          <EmptyState
            icon="alert-triangle"
            title="Unable to load the schedule"
            description={error}
            action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
          />
        </Panel>
      ) : loading && lots.length === 0 ? (
        <Panel padded><ListSkeleton rows={4} /></Panel>
      ) : (
        <>
          {rules && <TimelineStrip lots={sortedLots} rules={rules} movable={date >= today} onMove={moveLot} />}

          <PageSection title={`Lots for this day (${sortedLots.length})`}>
            {sortedLots.length === 0 ? (
              <Panel padded>
                <EmptyState
                  icon="calendar"
                  title="No lots scheduled"
                  description="There are no lots scheduled on this day."
                />
              </Panel>
            ) : (
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                {sortedLots.map((lot) => (
                  <LotCard
                    key={lot.lotId}
                    lot={lot}
                    onReschedule={() => setRescheduleTarget(lot)}
                    onCancel={() => setCancelTarget(lot)}
                  />
                ))}
              </div>
            )}
          </PageSection>
        </>
      )}

      {!rules && !loading && !error && (
        <Notice tone="info">The schedule window could not be loaded, so the timeline strip is hidden.</Notice>
      )}

      <RescheduleDialog
        key={rescheduleTarget?.lotId ?? 'none'}
        lot={rescheduleTarget}
        open={rescheduleTarget !== null}
        rules={rules}
        onClose={() => setRescheduleTarget(null)}
        onSuccess={load}
      />

      <ConfirmDialog
        open={cancelTarget !== null}
        title={`Cancel ${cancelTarget?.subjectName}?`}
        description={
          <>
            All <strong>{cancelTarget?.occupied} horse sessions</strong> in this lot will also be cancelled. The{' '}
            {cancelTarget && `${formatTime(cancelTarget.startTime)}–${formatTime(cancelTarget.endTime)}`} time slot will be released.
          </>
        }
        tone="danger"
        confirmLabel="Confirm cancellation"
        loading={cancelling}
        onConfirm={confirmCancel}
        onCancel={() => setCancelTarget(null)}
      />
    </ScreenLayout>
  );
}
