'use client';

import { useState } from 'react';
import { Button, FormField, Input, Modal, Notice, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { trainingService } from '../services/trainingService';
import {
  EMPTY_READINGS,
  READING_FIELDS,
  readingsToInputs,
  readingsToRequest,
  validateReadings,
  type ReadingErrors,
} from '../lib/workoutReadings';
import type { SessionHorse } from '../types';

const FORM_ID = 'session-result-form';

/**
 * Records the result of one horse in a Held Session. The form opens empty: the Trainer types the readings from the IoT device, or
 * presses "Generate sample data" for a demo. The rating is never sent; the server computes it. Recording changes no status.
 */
export function SessionResultDialog({
  horse,
  onClose,
  onRecorded,
}: {
  horse: SessionHorse | null;
  onClose: () => void;
  onRecorded: () => Promise<void> | void;
}) {
  return (
    <Modal
      open={horse !== null}
      onClose={onClose}
      title={horse ? `Result for ${horse.horseName}` : ''}
      size="md"
      layer="confirm"
    >
      {horse && <ResultForm key={horse.workoutId} horse={horse} onClose={onClose} onRecorded={onRecorded} />}
    </Modal>
  );
}

function ResultForm({
  horse,
  onClose,
  onRecorded,
}: {
  horse: SessionHorse;
  onClose: () => void;
  onRecorded: () => Promise<void> | void;
}) {
  const [readings, setReadings] = useState(EMPTY_READINGS);
  const [feedback, setFeedback] = useState('');
  const [fieldErrors, setFieldErrors] = useState<ReadingErrors>({});
  const [error, setError] = useState<string | null>(null);
  const [generating, setGenerating] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function generate() {
    setGenerating(true);
    setError(null);
    try {
      const sample = await trainingService.previewSessionResult(horse.workoutId);
      setReadings(readingsToInputs(sample));
      setFieldErrors({});
    } catch (cause) {
      setError(displayError(cause, 'Unable to generate sample data.'));
    } finally {
      setGenerating(false);
    }
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    const errors = validateReadings(readings);
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) return;
    setSubmitting(true);
    setError(null);
    try {
      const saved = await trainingService.recordSessionResult(horse.workoutId, readingsToRequest(readings, feedback));
      toast.success(
        saved.performanceRating != null
          ? `Result recorded. Rating ${saved.performanceRating} / 10.`
          : 'Result recorded.',
      );
      await onRecorded();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Unable to record the result.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form id={FORM_ID} onSubmit={submit} noValidate className="space-y-4">
      <div className="flex justify-end">
        <Button type="button" size="sm" variant="secondary" icon="sparkles" onClick={generate} disabled={generating || submitting}>
          {generating ? 'Generating...' : 'Generate sample data'}
        </Button>
      </div>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        {READING_FIELDS.map((field) => (
          <FormField key={field.key} label={`${field.label} (${field.unit})`} required error={fieldErrors[field.key]}>
            <Input
              type="number"
              inputMode="decimal"
              step={field.step}
              value={readings[field.key]}
              onChange={(event) => setReadings((current) => ({ ...current, [field.key]: event.target.value }))}
            />
          </FormField>
        ))}
      </div>
      <FormField label="Feedback">
        <Textarea value={feedback} maxLength={2000} onChange={(event) => setFeedback(event.target.value)} />
      </FormField>
      {error && <Notice tone="error">{error}</Notice>}
      <div className="flex justify-end gap-2">
        <Button type="button" variant="tertiary" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button type="submit" variant="primary" disabled={submitting}>
          {submitting ? 'Saving...' : 'Record result'}
        </Button>
      </div>
    </form>
  );
}
