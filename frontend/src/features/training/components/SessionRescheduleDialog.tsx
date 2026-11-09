'use client';

import { useState } from 'react';
import { Button, FormField, Modal, Notice, Select } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { useTrainingRules } from '../hooks/useTrainingRules';
import { minutesOf, sessionStartOptions } from '../lib/timeline';
import { trainingService } from '../services/trainingService';
import type { SessionSummary } from '../types';

/**
 * Moves a Session to another start, picked from the quarter-hour starts that keep it inside the Golden Window. The backend still
 * checks overlaps with the Trainer's other Sessions and refuses once the Session has started.
 */
export function SessionRescheduleDialog({
  session,
  onClose,
  onMoved,
}: {
  session: SessionSummary | null;
  onClose: () => void;
  onMoved: () => Promise<void> | void;
}) {
  return (
    <Modal open={session !== null} onClose={onClose} title="Move session" size="sm" layer="confirm">
      {session && <MoveForm key={session.id} session={session} onClose={onClose} onMoved={onMoved} />}
    </Modal>
  );
}

function MoveForm({
  session,
  onClose,
  onMoved,
}: {
  session: SessionSummary;
  onClose: () => void;
  onMoved: () => Promise<void> | void;
}) {
  const rules = useTrainingRules();
  const [startTime, setStartTime] = useState(session.startTime.slice(0, 5));
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const duration = minutesOf(session.endTime) - minutesOf(session.startTime);
  const options = rules
    ? sessionStartOptions(rules.goldenHoursStart, rules.goldenHoursEnd, duration, rules.sessionStartStepMinutes)
    : [startTime];

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await trainingService.rescheduleSession(session.id, `${startTime}:00`);
      toast.success(`Moved to ${startTime}.`);
      await onMoved();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Unable to move the session.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      <FormField label="Start time">
        <Select value={startTime} onChange={(event) => setStartTime(event.target.value)}>
          {options.map((option) => (
            <option key={option} value={option}>{option}</option>
          ))}
        </Select>
      </FormField>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="flex justify-end gap-2">
        <Button type="button" variant="tertiary" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={submitting || startTime === session.startTime.slice(0, 5)}>
          {submitting ? 'Moving...' : 'Move'}
        </Button>
      </div>
    </form>
  );
}
