'use client';

import { useState } from 'react';
import { Button, FormField, Input, Modal, Notice, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { trainingService } from '../../services/trainingService';
import {
  EMPTY_READINGS,
  READING_FIELDS,
  readingsToInputs,
  readingsToRequest,
  validateReadings,
  type ReadingErrors,
  type ReadingKey,
} from '../../lib/workoutReadings';
import type { SessionHorse } from '../../types';

const FORM_ID = 'session-result-form';

/**
 * Records or edits the result of one horse in a Held Session. A new result opens an empty form: the Trainer types the readings from the
 * IoT device, or presses "Generate sample data" for a demo. Editing opens the same form pre-filled with the stored readings. The rating
 * is never sent; the server computes it on every save. Saving changes no status.
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
      title={horse ? `${horse.result ? 'Edit result' : 'Result'} for ${horse.horseName}` : ''}
      size="lg"
      layer="confirm"
    >
      {horse && <ResultForm key={horse.workoutId} horse={horse} onClose={onClose} onRecorded={onRecorded} />}
    </Modal>
  );
}

const MOVEMENT_KEYS = ['actualDistanceMeters', 'actualDurationMinutes', 'averageSpeedKmh', 'topSpeedKmh'] as const;
const HEART_KEYS = ['averageHeartRate', 'maxHeartRate', 'recoveryHeartRate'] as const;

function ResultForm({
  horse,
  onClose,
  onRecorded,
}: {
  horse: SessionHorse;
  onClose: () => void;
  onRecorded: () => Promise<void> | void;
}) {
  const stored = horse.result;
  const editing = stored !== null;
  const [readings, setReadings] = useState(() => (stored ? readingsToInputs(stored as Record<ReadingKey, number>) : EMPTY_READINGS));
  const [feedback, setFeedback] = useState(stored?.trainerFeedback ?? '');
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
      const request = readingsToRequest(readings, feedback);
      const saved = editing
        ? await trainingService.updateSessionResult(horse.workoutId, request)
        : await trainingService.recordSessionResult(horse.workoutId, request);
      const verb = editing ? 'updated' : 'recorded';
      toast.success(
        saved.performanceRating != null ? `Result ${verb}. Rating ${saved.performanceRating} / 10.` : `Result ${verb}.`,
      );
      await onRecorded();
      onClose();
    } catch (cause) {
      setError(displayError(cause, editing ? 'Unable to update the result.' : 'Unable to record the result.'));
    } finally {
      setSubmitting(false);
    }
  }

  const movementFields = READING_FIELDS.filter((f) =>
    (MOVEMENT_KEYS as readonly string[]).includes(f.key),
  );
  const heartFields = READING_FIELDS.filter((f) =>
    (HEART_KEYS as readonly string[]).includes(f.key),
  );

  return (
    <form id={FORM_ID} onSubmit={submit} noValidate className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3">
        <p className="text-xs text-[var(--color-text-secondary)]">
          Copy metrics from the tracking device or generate sample data for this workout.
        </p>
        <Button
          type="button"
          size="sm"
          variant="secondary"
          icon="sparkles"
          onClick={generate}
          disabled={generating || submitting}
          className="shrink-0"
        >
          {generating ? 'Generating...' : 'Generate sample data'}
        </Button>
      </div>

      <div className="space-y-4">
        <div className="space-y-2">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
            Distance & Speed
          </h4>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {movementFields.map((field) => (
              <FormField
                key={field.key}
                label={`${field.label} (${field.unit})`}
                required
                error={fieldErrors[field.key]}
              >
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
        </div>

        <div className="space-y-2">
          <h4 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
            Heart Rate (bpm)
          </h4>
          <div className="grid grid-cols-1 gap-3 sm:grid-cols-3">
            {heartFields.map((field) => (
              <FormField
                key={field.key}
                label={`${field.label} (${field.unit})`}
                required
                error={fieldErrors[field.key]}
                className="[&>label]:min-h-[2rem] [&>label]:flex [&>label]:items-end"
              >
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
        </div>
      </div>

      <FormField label="Feedback">
        <Textarea
          placeholder="Add trainer notes or feedback on horse performance..."
          value={feedback}
          maxLength={2000}
          rows={3}
          onChange={(event) => setFeedback(event.target.value)}
        />
      </FormField>

      {error && <Notice tone="error">{error}</Notice>}

      <div className="flex justify-end gap-2 border-t border-[var(--color-border)] pt-3">
        <Button type="button" variant="tertiary" onClick={onClose} disabled={submitting}>
          Cancel
        </Button>
        <Button type="submit" variant="primary" disabled={submitting}>
          {submitting ? 'Saving...' : editing ? 'Save changes' : 'Record result'}
        </Button>
      </div>
    </form>
  );
}
