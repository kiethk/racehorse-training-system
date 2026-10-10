'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Input } from '@/components/ui/Input';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { trainingApi } from '../services/trainingService';
import type { TrainingLotResponse } from '../types';
import { RescheduleDialog } from './RescheduleDialog';
import { displayError, formatDate } from '@/lib/display';
import { toast } from '@/lib/toast';

const WINDOW_START = 6 * 60; // 06:00 (360')
const WINDOW_END = 10 * 60;  // 10:00 (600')
const WINDOW_LEN = WINDOW_END - WINDOW_START; // 240'

function toMinutes(hhmmss: string): number {
  const [h, m] = hhmmss.split(':').map(Number);
  return h * 60 + m;
}

export function LotTimeline() {
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0],
  );
  const [lots, setLots] = useState<TrainingLotResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modals
  const [rescheduleLot, setRescheduleLot] = useState<TrainingLotResponse | null>(null);
  const [cancelTargetLot, setCancelTargetLot] = useState<TrainingLotResponse | null>(null);
  const [cancelling, setCancelling] = useState(false);

  const loadLots = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      // includeCancelled defaults to false per Plan 6
      const data = await trainingApi.getLots(selectedDate, selectedDate, false);
      setLots(data);
    } catch (err) {
      console.error('Unable to load the lot schedule:', err);
      setError(displayError(err, 'Unable to load training lots for the selected date.'));
    } finally {
      setLoading(false);
    }
  }, [selectedDate]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadLots();
  }, [loadLots]);

  async function handleConfirmCancel() {
    if (!cancelTargetLot) return;
    setCancelling(true);
    try {
      await trainingApi.cancelLot(cancelTargetLot.lotId);
      setCancelTargetLot(null);
      await loadLots();
    } catch (err) {
      toast.error(displayError(err, 'Unable to cancel this lot.'));
    } finally {
      setCancelling(false);
    }
  }

  // Sort lots by start time
  const sortedLots = useMemo(() => {
    return [...lots].sort((a, b) => a.startTime.localeCompare(b.startTime));
  }, [lots]);

  // Total minutes used within the golden-hour window
  const totalOccupiedMinutes = useMemo(() => {
    return lots.reduce((acc, l) => acc + (l.durationMinutes || 0), 0);
  }, [lots]);

  if (loading) return <ListSkeleton rows={4} />;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-[var(--color-text-primary)]">
            Daily training schedule
          </h1>
          <p className="text-xs text-[var(--color-text-secondary)]">
            View training lots between 06:00 and 10:00 and identify available time slots.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-xs font-medium text-[var(--color-text-primary)]">Date</span>
          <Input
            type="date"
            aria-label="Date"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="min-h-8 w-auto"
          />
          <Button variant="secondary" size="sm" onClick={() => loadLots()}>
            Refresh
          </Button>
        </div>
      </div>

      {error ? (
        <Panel padded>
          <EmptyState icon="alert-triangle" title="Unable to load data" description={error} />
        </Panel>
      ) : (
        <>
          {/* Visual timeline bar */}
          <Panel padded>
            <div className="flex items-center justify-between mb-3 text-sm">
              <span className="font-semibold text-[var(--color-text-primary)]">
                Schedule window 06:00–10:00 ({formatDate(selectedDate)})
              </span>
              <span className="text-xs text-[var(--color-text-secondary)]">
                Used: <strong>{totalOccupiedMinutes}/240 minutes</strong> · Available:{' '}
                <strong>{Math.max(0, 240 - totalOccupiedMinutes)} minutes</strong>
              </span>
            </div>

            {/* Hour ruler */}
            <div className="relative h-6 text-xs text-[var(--color-text-muted)] font-metric border-b border-[var(--color-border)] mb-2">
              <span className="absolute left-0">06:00</span>
              <span className="absolute left-[25%] -translate-x-1/2">07:00</span>
              <span className="absolute left-[50%] -translate-x-1/2">08:00</span>
              <span className="absolute left-[75%] -translate-x-1/2">09:00</span>
              <span className="absolute right-0">10:00</span>
            </div>

            {/* Visual strip of the lots */}
            <div className="relative h-16 w-full rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] border border-[var(--color-border)] overflow-hidden">
              {sortedLots.map((lot) => {
                const startMins = toMinutes(lot.startTime);
                const endMins = toMinutes(lot.endTime);
                const leftPercent = Math.max(0, ((startMins - WINDOW_START) / WINDOW_LEN) * 100);
                const widthPercent = Math.min(100 - leftPercent, ((endMins - startMins) / WINDOW_LEN) * 100);

                const tone =
                  lot.remainingSlots === 0
                    ? 'bg-[var(--color-danger-soft)] border-[var(--color-danger)] text-[var(--color-danger)]'
                    : lot.remainingSlots <= 2
                    ? 'bg-[var(--color-warning-soft)] border-[var(--color-warning)] text-[var(--color-warning)]'
                    : 'bg-[var(--color-success-soft)] border-[var(--color-success)] text-[var(--color-success)]';

                return (
                  <div
                    key={lot.lotId}
                    style={{ left: `${leftPercent}%`, width: `${widthPercent}%` }}
                    className={`absolute top-1 bottom-1 rounded border px-2 py-1 flex flex-col justify-center overflow-hidden transition shadow-[var(--shadow-panel)] ${tone}`}
                    title={`Lot #${lot.lotId}: ${lot.subjectName} (${lot.startTime} - ${lot.endTime}) - ${lot.occupied}/${lot.maxCapacity} horses`}
                  >
                    <div className="text-xs font-bold truncate">
                      #{lot.lotId} {lot.subjectName}
                    </div>
                    <div className="text-xs truncate">
                      {lot.startTime.substring(0, 5)}–{lot.endTime.substring(0, 5)} ({lot.occupied}/{lot.maxCapacity} horses)
                    </div>
                  </div>
                );
              })}
            </div>
          </Panel>

          {/* Detail cards for each lot */}
          <div className="space-y-3">
            <h2 className="text-lg font-semibold text-[var(--color-text-primary)]">
              Lots for this day ({sortedLots.length})
            </h2>

            {sortedLots.length === 0 ? (
              <Panel padded>
                <EmptyState
                  icon="activity"
                  title="No lots scheduled"
                  description="There are no lots scheduled between 06:00 and 10:00 on this day."
                />
              </Panel>
            ) : (
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                {sortedLots.map((lot) => (
                  <Panel key={lot.lotId} padded>
                    <div className="flex flex-col justify-between h-full">
                      <div>
                        <div className="flex items-start justify-between gap-2">
                          <div>
                            <div className="flex items-center gap-2">
                              <span className="text-base font-bold text-[var(--color-text-primary)]">
                                Lot #{lot.lotId} • {lot.subjectName}
                              </span>
                            </div>
                            <div className="text-xs text-[var(--color-text-secondary)] mt-0.5">
                              <strong>{lot.startTime} – {lot.endTime}</strong> ({lot.durationMinutes} min)
                            </div>
                          </div>

                          <Pill
                            tone={
                              lot.remainingSlots === 0
                                ? 'danger'
                                : lot.remainingSlots <= 2
                                ? 'warning'
                                : 'success'
                            }
                            size="sm"
                          >
                            {lot.occupied}/{lot.maxCapacity} horses {lot.remainingSlots === 0 ? '(Full)' : ''}
                          </Pill>
                        </div>

                        <div className="mt-3 text-xs border-t border-[var(--color-border)] pt-2">
                          <div className="text-xs text-[var(--color-text-muted)]">
                          Participating horses ({lot.horseNames.length}):
                          </div>
                          <div className="mt-1 flex flex-wrap gap-1.5">
                            {lot.horseNames.length === 0 ? (
                              <span className="text-xs text-[var(--color-text-muted)] italic">
                                No horses
                              </span>
                            ) : (
                              lot.horseNames.map((name, i) => (
                                <span
                                  key={i}
                                  className="rounded bg-[var(--color-surface-muted)] px-2 py-0.5 text-xs font-medium text-[var(--color-text-primary)] border border-[var(--color-border)]"
                                >
                                  🏇 {name}
                                </span>
                              ))
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="mt-4 pt-2 border-t border-[var(--color-border)] flex justify-end gap-2">
                        <Button
                          variant="secondary"
                          size="sm"
                          onClick={() => setRescheduleLot(lot)}
                        >
                          Reschedule
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => setCancelTargetLot(lot)}
                        >
                          Cancel lot
                        </Button>
                      </div>
                    </div>
                  </Panel>
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {/* Reschedule modal */}
      <RescheduleDialog
        lot={rescheduleLot}
        open={rescheduleLot !== null}
        onClose={() => setRescheduleLot(null)}
        onSuccess={loadLots}
      />

      {/* Cancel-lot confirm dialog */}
      <ConfirmDialog
        open={cancelTargetLot !== null}
        title={`Cancel lot #${cancelTargetLot?.lotId} (${cancelTargetLot?.subjectName})?`}
        description={
          <div>
            All <strong>{cancelTargetLot?.occupied} horse sessions</strong> in this lot will also be cancelled.
            The {cancelTargetLot?.startTime}–{cancelTargetLot?.endTime} time slot will be released.
          </div>
        }
        tone="danger"
        confirmLabel="Confirm cancellation"
        loading={cancelling}
        onConfirm={handleConfirmCancel}
        onCancel={() => setCancelTargetLot(null)}
      />
    </div>
  );
}
