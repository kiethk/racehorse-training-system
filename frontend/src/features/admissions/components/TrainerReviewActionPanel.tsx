'use client';

import { useState } from 'react';
import { Button, FormField, Notice, Textarea } from '@/components/ui';
import { formatDateTime } from '@/lib/display';
import { AdmissionSideCard } from '../shared/components/AdmissionInfoSection';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import { simpleStatusLabel } from '../shared/components/AdmissionStatusBadge';
import type { TrainerAdmissionView } from '../types/trainer';

// Matches TrainingLimits.MAX_ADMISSION_TRAINER_FEEDBACK_LENGTH on the backend.
const MAX_FEEDBACK = 3000;

interface TrainerReviewActionPanelProps {
  view: TrainerAdmissionView;
  onSuccess: () => void;
}

export function TrainerReviewActionPanel({ view, onSuccess }: TrainerReviewActionPanelProps) {
  const { admission, horse } = view;
  const submitted = admission.trainerReviewedAt != null;
  const horseMissing = horse === null;
  const canReview = admission.status === 'TRAINER_REVIEW' && !submitted;

  const [feedback, setFeedback] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submittedNotice, setSubmittedNotice] = useState(false);

  async function handleSubmit() {
    setFormError(null);

    const trimmed = feedback.trim();
    if (!trimmed) {
      setFormError('Please write your feedback.');
      return;
    }
    if (trimmed.length > MAX_FEEDBACK) {
      setFormError(`Feedback must not exceed ${MAX_FEEDBACK} characters (currently ${trimmed.length}).`);
      return;
    }

    try {
      setSubmitting(true);
      await trainerAdmissionsApi.submitReview(admission.admissionId, { trainerFeedback: trimmed });
      setSubmittedNotice(true);
      onSuccess();
    } catch (err: unknown) {
      setFormError(err instanceof Error ? err.message : 'Failed to submit feedback.');
    } finally {
      setSubmitting(false);
    }
  }

  // Already submitted -> show the feedback (read-only)
  if (submitted) {
    return (
      <AdmissionSideCard title="Trainer feedback (submitted)">
        <p className="text-[var(--color-text-muted)]">
          {formatDateTime(admission.trainerReviewedAt)}
        </p>
        {admission.trainerFeedback && (
          <p className="whitespace-pre-wrap text-[var(--color-text-primary)]">
            {admission.trainerFeedback}
          </p>
        )}
      </AdmissionSideCard>
    );
  }

  // Not currently in TRAINER_REVIEW status
  if (!canReview) {
    return (
      <AdmissionSideCard title="Trainer feedback">
        <p className="text-[var(--color-text-muted)]">
          This application is not currently waiting for Trainer feedback (current status: {simpleStatusLabel(admission.status)}).
        </p>
      </AdmissionSideCard>
    );
  }

  // Pending TRAINER_REVIEW but horse record is missing
  if (horseMissing) {
    return (
      <AdmissionSideCard title="Trainer feedback">
        <Notice tone="warning">
          Cannot review yet: the candidate horse profile has not been created by the Groom.
        </Notice>
      </AdmissionSideCard>
    );
  }

  return (
    <AdmissionSideCard title="Trainer feedback" tone="primary" className="space-y-4">
      {submittedNotice && <Notice tone="success">Feedback sent to the Club Manager.</Notice>}

      <FormField label="Feedback" required>
        <Textarea
          rows={6}
          maxLength={MAX_FEEDBACK}
          value={feedback}
          onChange={(e) => setFeedback(e.target.value)}
        />
      </FormField>

      {formError && <Notice tone="error">{formError}</Notice>}

      <Button variant="primary" className="w-full" loading={submitting} onClick={handleSubmit}>
        Submit feedback
      </Button>
    </AdmissionSideCard>
  );
}
