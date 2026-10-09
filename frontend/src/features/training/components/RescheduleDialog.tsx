'use client';

import { useState } from 'react';
import { Button, FormField, Input, Modal, Notice } from '@/components/ui';
import { displayError } from '@/lib/display';
import { trainingApi } from '../services/trainingService';
import type { TrainingLotResponse } from '../types';

interface RescheduleDialogProps {
  lot: TrainingLotResponse | null;
  open: boolean;
  onClose: () => void;
  onSuccess: () => Promise<void> | void;
}

const FORM_ID = 'reschedule-lot-form';
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

    // Block early on the client (BR-03: golden-hour window 06:00 - 10:00)
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
    <Modal
      open
      onClose={onClose}
      size="sm"
      dismissible={!submitting}
      title={`Reschedule lot #${lot.lotId} (${lot.subjectName})`}
      description={`All ${lot.occupied} horses in this lot will move to the new time slot.`}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" form={FORM_ID} disabled={submitting}>
            {submitting ? 'Rescheduling…' : 'Confirm reschedule'}
          </Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleReschedule} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        <div className="rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-3 text-xs">
          <div>Session duration: <strong>{duration} minutes</strong></div>
          <div>Current time: <strong>{lot.startTime} – {lot.endTime}</strong></div>
        </div>

        <FormField label="New start time (06:00–10:00)" required hint={`Estimated end time: ${newEndTime}`}>
          <Input
            type="time"
            min="06:00"
            max="10:00"
            value={newStartTime}
            onChange={(e) => setNewStartTime(e.target.value)}
          />
        </FormField>

        <FormField label="Reason for rescheduling">
          <Input
            type="text"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="For example: weather conditions, track maintenance..."
          />
        </FormField>
      </form>
    </Modal>
  );
}
