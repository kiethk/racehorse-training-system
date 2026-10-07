'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';
import { displayError } from '@/lib/display';
import { controlClassName, FormField } from '@/components/ui/FormField';
import { trainingApi } from '../services/trainingService';
import type { TrainingLotResponse } from '../types';

interface RescheduleDialogProps {
  lot: TrainingLotResponse | null;
  open: boolean;
  onClose: () => void;
  onSuccess: () => Promise<void> | void;
}

const WINDOW_START = 6 * 60; // 06:00
const WINDOW_END = 10 * 60; // 10:00

function toMinutes(hhmmss: string): number {
  const [h, m] = hhmmss.split(':').map(Number);
  return h * 60 + m;
}

function addMinutes(hhmm: string, mins: number): string {
  const total = toMinutes(hhmm) + mins;
  const h = Math.floor(total / 60);
  const m = total % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

export function RescheduleDialog({
  lot,
  open,
  onClose,
  onSuccess,
}: RescheduleDialogProps) {
  const [newStartTime, setNewStartTime] = useState(
    lot?.startTime ? lot.startTime.substring(0, 5) : '06:00',
  );
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open || !lot) return null;

  const duration = lot.durationMinutes || 60;
  const newEndTime = addMinutes(newStartTime, duration);

  async function handleReschedule(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    // Chặn trước tại client (BR-03: Khung giờ vàng 06:00 - 10:00)
    const startMins = toMinutes(newStartTime);
    const endMins = toMinutes(newEndTime);
    if (startMins < WINDOW_START || endMins > WINDOW_END) {
      setError(
        `Sessions can only be scheduled between 06:00 and 10:00. The selected time (${newStartTime}–${newEndTime}) is outside this window.`,
      );
      return;
    }

    setSubmitting(true);
    try {
      if (!lot) return;
      await trainingApi.rescheduleLot(lot.lotId, {
        newStartTime: `${newStartTime}:00`,
        reason: reason.trim() || undefined,
      });
      await onSuccess();
      onClose();
    } catch (err) {
      setError(displayError(err, 'Unable to reschedule this lot.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div
        className="absolute inset-0 bg-black/30 backdrop-blur-sm"
        onClick={() => !submitting && onClose()}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        className="relative w-full max-w-md rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 shadow-xl"
      >
        <h2 className="text-[16px] font-semibold text-[var(--color-text-primary)]">
          Reschedule lot #{lot.lotId} ({lot.subjectName})
        </h2>
        <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
          All {lot.occupied} horses in this lot will move to the new time slot.
        </p>

        {error && <Notice tone="error" className="mt-3">{error}</Notice>}

        <form onSubmit={handleReschedule} className="mt-4 space-y-4">
          <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-[12px]">
            <div>Session duration: <strong>{duration} minutes</strong></div>
            <div>Current time: <strong>{lot.startTime} – {lot.endTime}</strong></div>
          </div>

          <FormField label="New start time (06:00–10:00)" htmlFor="lot-start-time" required hint={`Estimated end time: ${newEndTime}`}>
            <input
              id="lot-start-time"
              type="time"
              required
              min="06:00"
              max="10:00"
              value={newStartTime}
              onChange={(e) => setNewStartTime(e.target.value)}
              className={controlClassName}
              aria-describedby="lot-start-time-description"
            />
          </FormField>

          <FormField label="Reason for rescheduling" htmlFor="lot-reschedule-reason">
            <input
              id="lot-reschedule-reason"
              type="text"
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              placeholder="For example: weather conditions, track maintenance..."
              className={controlClassName}
            />
          </FormField>

          <div className="flex justify-end gap-2 pt-2 border-t border-[var(--color-border)]">
            <Button variant="secondary" size="sm" type="button" onClick={onClose} disabled={submitting}>
              Cancel
            </Button>
            <Button variant="primary" size="sm" type="submit" disabled={submitting}>
              {submitting ? 'Rescheduling…' : 'Confirm reschedule'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
