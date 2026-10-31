'use client';

import { useEffect, useState } from 'react';
import { Button, FormField, Modal, Notice, Skeleton, Textarea } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { calculatePerformanceRating } from '../lib/performanceCalculator';
import { trainingService } from '../services/trainingService';
import type { SessionHorse, WorkoutResultPreviewResponse } from '../types';

const FORM_ID = 'session-result-form';

function Measure({ label, value, unit }: { label: string; value: number | string; unit: string }) {
  return (
    <div>
      <dt className="text-xs text-[var(--color-text-muted)]">{label}</dt>
      <dd className="text-sm font-semibold text-[var(--color-text-primary)]">{value} {unit}</dd>
    </div>
  );
}

/**
 * Records the result of one horse in a Held Session. The measurements come from the backend preview (a fresh simulated set that is
 * not saved until the Trainer confirms), so the Trainer only adds feedback. Recording changes no status.
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
  const [preview, setPreview] = useState<WorkoutResultPreviewResponse | null>(null);
  const [feedback, setFeedback] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let current = true;
    trainingService
      .previewSessionResult(horse.workoutId)
      .then((data) => current && setPreview(data))
      .catch((cause) => current && setError(displayError(cause, 'Unable to prepare the result.')));
    return () => {
      current = false;
    };
  }, [horse.workoutId]);

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    if (!preview) return;
    setSubmitting(true);
    setError(null);
    try {
      await trainingService.recordSessionResult(horse.workoutId, {
        actualDistanceMeters: preview.actualDistanceMeters,
        actualDurationMinutes: preview.actualDurationMinutes,
        topSpeedKmh: preview.topSpeedKmh,
        averageSpeedKmh: preview.averageSpeedKmh,
        averageHeartRate: preview.averageHeartRate,
        maxHeartRate: preview.maxHeartRate,
        recoveryHeartRate: preview.recoveryHeartRate,
        performanceRating: rating,
        trainerFeedback: feedback.trim() || null,
      });
      toast.success('Result recorded.');
      await onRecorded();
      onClose();
    } catch (cause) {
      setError(displayError(cause, 'Unable to record the result.'));
    } finally {
      setSubmitting(false);
    }
  }

  if (!preview) {
    return error ? <Notice tone="error">{error}</Notice> : <Skeleton className="h-40 w-full" />;
  }
  const rating = preview.performanceRating ?? calculatePerformanceRating({
    actualDistanceMeters: Number(preview.actualDistanceMeters),
    actualDurationMinutes: Number(preview.actualDurationMinutes),
    topSpeedKmh: Number(preview.topSpeedKmh),
    averageSpeedKmh: Number(preview.averageSpeedKmh),
    averageHeartRate: preview.averageHeartRate,
    maxHeartRate: preview.maxHeartRate,
    recoveryHeartRate: preview.recoveryHeartRate,
  });

  return (
    <form id={FORM_ID} onSubmit={submit} className="space-y-4">
      <dl className="grid grid-cols-2 gap-3 sm:grid-cols-3">
        <Measure label="Distance" value={preview.actualDistanceMeters} unit="m" />
        <Measure label="Duration" value={preview.actualDurationMinutes} unit="min" />
        <Measure label="Top speed" value={preview.topSpeedKmh} unit="km/h" />
        <Measure label="Average speed" value={preview.averageSpeedKmh} unit="km/h" />
        <Measure label="Average heart rate" value={preview.averageHeartRate} unit="bpm" />
        <Measure label="Max heart rate" value={preview.maxHeartRate} unit="bpm" />
        <Measure label="Recovery heart rate" value={preview.recoveryHeartRate} unit="bpm" />
        <Measure label="Rating" value={rating} unit="/ 10" />
      </dl>
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
