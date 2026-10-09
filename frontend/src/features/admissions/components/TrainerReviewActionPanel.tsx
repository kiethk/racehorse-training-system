'use client';

import { useState } from 'react';
import { Button, Checkbox, FormField, Input, Notice, Textarea } from '@/components/ui';
import { AdmissionSideCard, InfoRow } from '../shared/components/AdmissionInfoSection';
import { trainerAdmissionsApi } from '../services/trainerAdmissionService';
import { simpleStatusLabel } from '../shared/components/AdmissionStatusBadge';
import type {
  RacingReadinessStatus,
  TrainerAdmissionView,
  TrainerReviewRequest,
} from '../types/trainer';

const READINESS_OPTIONS: {
  value: RacingReadinessStatus;
  label: string;
}[] = [
  { value: 'READY', label: 'Ready for Racing' },
  { value: 'NEEDS_MORE_TRAINING', label: 'Needs More Training' },
  { value: 'UNSUITABLE', label: 'Unsuitable' },
];

const MIN_REMARKS = 20;

function fmtScore(score: number | null | undefined): string {
  return score != null ? `${score}/10` : '—';
}

function ScoreInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number | null | undefined;
  onChange: (raw: string) => void;
}) {
  return (
    <FormField label={`${label} (0–10)`}>
      <Input type="number" min={0} max={10} step={0.5} value={value ?? ''} onChange={(e) => onChange(e.target.value)} />
    </FormField>
  );
}

interface TrainerReviewActionPanelProps {
  view: TrainerAdmissionView;
  onSuccess: () => void;
}

export function TrainerReviewActionPanel({ view, onSuccess }: TrainerReviewActionPanelProps) {
  const { admission, horse, existingAssessment } = view;
  const readOnly = existingAssessment !== null;
  const horseMissing = horse === null;
  const canReview = admission.status === 'TRAINER_REVIEW' && !readOnly;

  const [form, setForm] = useState<TrainerReviewRequest>({
    readinessStatus: 'NEEDS_MORE_TRAINING',
    conformationScore: null,
    temperamentScore: null,
    gaitQualityScore: null,
    estimatedMonthsToRace: null,
    remarks: '',
  });

  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [submittedNotice, setSubmittedNotice] = useState(false);

  const setNum =
    (key: keyof TrainerReviewRequest) =>
    (raw: string) =>
      setForm((f) => ({ ...f, [key]: raw === '' ? null : Number(raw) }));

  async function handleSubmit() {
    setFormError(null);

    const remarks = form.remarks?.trim() ?? '';
    if (remarks.length < MIN_REMARKS) {
      setFormError(
        `Professional remarks must be at least ${MIN_REMARKS} characters (currently ${remarks.length}).`,
      );
      return;
    }

    try {
      setSubmitting(true);
      await trainerAdmissionsApi.submitReview(admission.admissionId, {
        ...form,
        remarks,
      });
      setSubmittedNotice(true);
      onSuccess();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to submit evaluation.';
      setFormError(msg);
    } finally {
      setSubmitting(false);
    }
  }

  // Already evaluated -> Display evaluation results (Read-only)
  if (readOnly && existingAssessment) {
    return (
      <AdmissionSideCard title="Head Trainer evaluation (submitted)">
          <InfoRow label="Readiness Status" value={existingAssessment.readinessStatus} />
          <InfoRow label="Conformation Score" value={fmtScore(existingAssessment.conformationScore)} />
          <InfoRow label="Temperament Score" value={fmtScore(existingAssessment.temperamentScore)} />
          <InfoRow label="Gait Quality Score" value={fmtScore(existingAssessment.gaitQualityScore)} />
          <InfoRow
            label="Estimated Time to Race"
            value={
              existingAssessment.estimatedMonthsToRace != null
                ? `${existingAssessment.estimatedMonthsToRace} months`
                : '—'
            }
          />
          <InfoRow label="Assessment Date" value={existingAssessment.assessmentDate} />
          {existingAssessment.remarks && (
            <div className="border-t border-[var(--color-border)] pt-3">
              <span className="mb-1 block text-[var(--color-text-muted)]">Professional remarks</span>
              <p className="whitespace-pre-wrap text-[var(--color-text-primary)]">
                {existingAssessment.remarks}
              </p>
            </div>
          )}
      </AdmissionSideCard>
    );
  }

  // Not currently in TRAINER_REVIEW status
  if (!canReview) {
    return (
      <AdmissionSideCard title="Head Trainer evaluation">
        <p className="text-[var(--color-text-muted)]">
          This application is not currently pending Head Trainer evaluation (current status: {simpleStatusLabel(admission.status)}).
        </p>
      </AdmissionSideCard>
    );
  }

  // Pending TRAINER_REVIEW but horse record is missing
  if (horseMissing) {
    return (
      <AdmissionSideCard title="Head Trainer evaluation">
        <Notice tone="warning">
          Cannot evaluate yet: the candidate horse profile has not been created by the Groom.
        </Notice>
      </AdmissionSideCard>
    );
  }

  return (
    <AdmissionSideCard title="Racing readiness assessment" tone="primary" className="space-y-4">
      {submittedNotice && (
        <Notice tone="success">
          Your evaluation has been submitted successfully and forwarded to the Club Manager.
        </Notice>
      )}

      <fieldset className="space-y-2">
        <legend className="text-xs font-medium text-[var(--color-text-primary)]">Racing readiness status</legend>
        {READINESS_OPTIONS.map((opt) => (
          <Checkbox
            key={opt.value}
            type="radio"
            name="readiness"
            label={opt.label}
            checked={form.readinessStatus === opt.value}
            onChange={() => setForm((f) => ({ ...f, readinessStatus: opt.value }))}
          />
        ))}
      </fieldset>

      <ScoreInput label="Conformation" value={form.conformationScore} onChange={setNum('conformationScore')} />
      <ScoreInput label="Temperament" value={form.temperamentScore} onChange={setNum('temperamentScore')} />
      <ScoreInput label="Gait quality" value={form.gaitQualityScore} onChange={setNum('gaitQualityScore')} />

      <FormField label="Estimated months until eligible for race registration">
        <Input
          type="number"
          min={0}
          max={60}
          placeholder="Number of months"
          value={form.estimatedMonthsToRace ?? ''}
          onChange={(e) => setNum('estimatedMonthsToRace')(e.target.value)}
        />
      </FormField>

      <FormField
        label="Professional remarks"
        required
        hint={`${form.remarks?.trim().length ?? 0}/${MIN_REMARKS} characters minimum`}
      >
        <Textarea
          rows={4}
          placeholder="Balanced conformation with well-developed musculature. Calm temperament upon contact. Even strides and good reach..."
          value={form.remarks ?? ''}
          onChange={(e) => setForm((f) => ({ ...f, remarks: e.target.value }))}
        />
      </FormField>

      <p className="text-xs text-[var(--color-text-muted)]">
        Fitness metrics will be measured and assessed periodically once the horse completes quarantine and enters regular training.
      </p>

      {formError && <Notice tone="error">{formError}</Notice>}

      <Button variant="primary" className="w-full" loading={submitting} onClick={handleSubmit}>
        Submit evaluation
      </Button>
      <p className="text-xs text-[var(--color-text-muted)]">
        Once submitted, the evaluation will be forwarded to the Club Manager.
      </p>
    </AdmissionSideCard>
  );
}
