'use client';

import { useState } from 'react';
import { Button, FormField, Modal, Notice, Select } from '@/components/ui';
import { displayError, formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { clockOf, lotStartOptions, minutesOf } from '../lib/timeline';
import { trainingService } from '../services/trainingService';
import type { TrainingLotResponse, TrainingRules } from '../types';

const FORM_ID = 'reschedule-lot-form';

/**
 * Moves a whole lot to a new start time, picked from the quarter-hour starts that keep the lot inside the golden
 * window. It is the keyboard alternative to dragging the lot on the timeline; the backend still checks overlaps.
 */
export function RescheduleDialog({
  lot,
  open,
  rules,
  onClose,
  onSuccess,
}: {
  lot: TrainingLotResponse | null;
  open: boolean;
  rules: TrainingRules | null;
  onClose: () => void;
  onSuccess: () => Promise<void> | void;
}) {
  const [startTime, setStartTime] = useState(lot ? lot.startTime.slice(0, 5) : '');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open || !lot) return null;
  const target = lot;

  const duration = target.durationMinutes || 60;
  const options = rules
    ? lotStartOptions(rules.goldenHoursStart, rules.goldenHoursEnd, duration, rules.lotStartStepMinutes)
    : [startTime];

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await trainingService.rescheduleLot(target.lotId, { newStartTime: `${startTime}:00` });
      toast.success(`${target.subjectName} moved to ${startTime}.`);
      await onSuccess();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Unable to reschedule this lot.'));
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
      title={`Reschedule ${target.subjectName}`}
      description={`${formatTime(target.startTime)} – ${formatTime(target.endTime)} · ${target.occupied} horses move together`}
      footer={
        <>
          <Button onClick={onClose} disabled={submitting}>Cancel</Button>
          <Button variant="primary" type="submit" form={FORM_ID} loading={submitting}>Confirm reschedule</Button>
        </>
      }
    >
      <form id={FORM_ID} onSubmit={handleSubmit} className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}
        <FormField label="New start time" required hint={`Ends at ${clockOf(minutesOf(startTime) + duration)}`}>
          <Select value={startTime} onChange={(e) => setStartTime(e.target.value)}>
            {options.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </Select>
        </FormField>
      </form>
    </Modal>
  );
}
