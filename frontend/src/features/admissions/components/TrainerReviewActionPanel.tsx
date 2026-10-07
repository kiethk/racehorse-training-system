'use client';

import { useState } from 'react';
import { Button } from '@/components/ui/Button';
import { FieldLabel, Panel, SectionTitle } from '@/components/ui/Panel';
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

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-[var(--color-text-muted)]">{label}</span>
      <span className="font-medium text-[var(--color-text-primary)]">{value}</span>
    </div>
  );
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
    <div>
      <FieldLabel>{label} (0–10)</FieldLabel>
      <input
        type="number"
        min={0}
        max={10}
        step={0.5}
        className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-2 py-1 text-[13px]"
        value={value ?? ''}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
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
      <Panel padded className="bg-[var(--color-surface)]">
        <SectionTitle>Head Trainer Evaluation (Submitted)</SectionTitle>
        <div className="mt-3 space-y-2 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-[12px]">
          <Row label="Readiness Status" value={existingAssessment.readinessStatus} />
          <Row label="Conformation Score" value={fmtScore(existingAssessment.conformationScore)} />
          <Row label="Temperament Score" value={fmtScore(existingAssessment.temperamentScore)} />
          <Row label="Gait Quality Score" value={fmtScore(existingAssessment.gaitQualityScore)} />
          <Row
            label="Estimated Time to Race"
            value={
              existingAssessment.estimatedMonthsToRace != null
                ? `${existingAssessment.estimatedMonthsToRace} months`
                : '—'
            }
          />
          <Row label="Assessment Date" value={existingAssessment.assessmentDate} />
          {existingAssessment.remarks && (
            <div className="mt-3 border-t border-[var(--color-border)] pt-2">
              <span className="block text-[var(--color-text-muted)] mb-1">Professional Remarks:</span>
              <p className="whitespace-pre-wrap text-[var(--color-text-primary)]">
                {existingAssessment.remarks}
              </p>
            </div>
          )}
        </div>
      </Panel>
    );
  }

  // Not currently in TRAINER_REVIEW status
  if (!canReview) {
    return (
      <Panel padded className="bg-[var(--color-surface)]">
        <SectionTitle>Head Trainer Evaluation</SectionTitle>
        <p className="mt-2 text-[12px] text-[var(--color-text-muted)] italic">
          This application is not currently pending Head Trainer evaluation (Current status: {simpleStatusLabel(admission.status)}).
        </p>
      </Panel>
    );
  }

  // Pending TRAINER_REVIEW but horse record is missing
  if (horseMissing) {
    return (
      <Panel padded className="bg-[var(--color-surface)]">
        <SectionTitle>Head Trainer Evaluation</SectionTitle>
        <div className="mt-2 rounded-[var(--radius-md)] bg-[var(--color-warning-soft)] p-3 text-[12px] text-[var(--color-warning)]">
          Cannot evaluate yet: The candidate horse profile has not been created by the Groom.
        </div>
      </Panel>
    );
  }

  return (
    <Panel padded className="bg-[var(--color-surface)]">
      <SectionTitle>Racing Readiness Assessment</SectionTitle>

      {submittedNotice && (
        <div className="mt-3 rounded-[var(--radius-md)] bg-[var(--color-success-soft)] p-3 text-[12px] text-[var(--color-success)]">
          Your evaluation has been submitted successfully and forwarded to the Club Manager.
        </div>
      )}

      <div className="mt-4 space-y-4">
        {/* --- Readiness Status --- */}
        <div>
          <FieldLabel>Racing Readiness Status</FieldLabel>
          <div className="mt-1.5 flex flex-wrap gap-4">
            {READINESS_OPTIONS.map((opt) => (
              <label key={opt.value} className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="readiness"
                  className="mt-0.5"
                  checked={form.readinessStatus === opt.value}
                  onChange={() => setForm((f) => ({ ...f, readinessStatus: opt.value }))}
                />
                <span className="text-[13px] text-[var(--color-text-primary)] font-medium">
                  {opt.label}
                </span>
              </label>
            ))}
          </div>
        </div>

        {/* --- Observation Scores --- */}
        <div className="grid gap-4 sm:grid-cols-3">
          <ScoreInput
            label="Conformation"
            value={form.conformationScore}
            onChange={setNum('conformationScore')}
          />
          <ScoreInput
            label="Temperament"
            value={form.temperamentScore}
            onChange={setNum('temperamentScore')}
          />
          <ScoreInput
            label="Gait Quality"
            value={form.gaitQualityScore}
            onChange={setNum('gaitQualityScore')}
          />
        </div>

        {/* --- Estimated Months --- */}
        <div>
          <FieldLabel>Estimated Months Until Eligible for Race Registration</FieldLabel>
          <input
            type="number"
            min={0}
            max={60}
            placeholder="Number of months"
            className="mt-1 w-40 rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-1.5 text-[13px]"
            value={form.estimatedMonthsToRace ?? ''}
            onChange={(e) => setNum('estimatedMonthsToRace')(e.target.value)}
          />
        </div>

        {/* --- Remarks --- */}
        <div>
          <FieldLabel>Professional Remarks (minimum 20 characters)</FieldLabel>
          <textarea
            rows={4}
            placeholder="Balanced conformation with well-developed musculature. Calm temperament upon contact. Even strides and good reach..."
            className="mt-1 w-full rounded-[var(--radius-md)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] px-3 py-2 text-[13px]"
            value={form.remarks ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, remarks: e.target.value }))}
          />
          <div className="mt-1 text-[11px] text-[var(--color-text-muted)]">
            {form.remarks?.trim().length ?? 0}/{MIN_REMARKS} characters minimum
          </div>
        </div>

        <p className="text-[11px] text-[var(--color-text-muted)] italic">
          * Fitness metrics will be measured and assessed periodically once the horse completes quarantine and enters regular training.
        </p>

        {formError && (
          <div className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
            {formError}
          </div>
        )}

        <div className="flex items-center gap-3 pt-2">
          <Button
            variant="primary"
            disabled={submitting}
            onClick={handleSubmit}
          >
            {submitting ? 'Submitting...' : 'Submit Evaluation'}
          </Button>
          <span className="text-[11px] text-[var(--color-text-muted)]">
            Once submitted, the evaluation will be forwarded to the Club Manager.
          </span>
        </div>
      </div>
    </Panel>
  );
}
