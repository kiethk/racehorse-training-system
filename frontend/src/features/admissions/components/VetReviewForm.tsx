'use client';

import { useRef, useState, type FormEvent } from 'react';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Icon, type IconName } from '@/components/ui/Icon';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '../services/api';
import type {
  HorseHealthMetricRequest,
  VetDecision,
  VetReviewRequest,
  VetReviewResponse,
} from '../types';

type MetricField = keyof HorseHealthMetricRequest;

interface DecisionCardConfig {
  value: VetDecision;
  title: string;
  subtitle: string;
  description: string;
  outcomeNotes: string[];
  icon: IconName;
  tone: 'success' | 'warning' | 'danger';
  activeBorder: string;
  activeBg: string;
}

const decisions: DecisionCardConfig[] = [
  {
    value: 'APPROVED',
    title: 'Approve Vet Review',
    subtitle: 'Medically cleared for Trainer review',
    description:
      'The horse passes physical examination and biosecurity screening. The admission can proceed to Trainer review.',
    outcomeNotes: [
      'Vet decision is recorded as APPROVED',
      'Admission moves to TRAINER_REVIEW',
      'Horse remains a CANDIDATE in quarantine',
      'Training stays locked until final Manager approval',
    ],
    icon: 'check',
    tone: 'success',
    activeBorder: 'border-[var(--color-success)] ring-1 ring-[var(--color-success)]',
    activeBg: 'bg-[var(--color-success-soft)]',
  },
  {
    value: 'RECHECK_REQUIRED',
    title: 'Require Recheck',
    subtitle: 'Follow-up examination needed',
    description:
      'Condition requires continued monitoring, lab verification, or treatment completion before clearance.',
    outcomeNotes: [
      'Horse remains quarantined in isolation stall',
      'Training remains strictly locked',
      'Follow-up examination will be automatically scheduled',
      'A future follow-up date is required',
    ],
    icon: 'calendar',
    tone: 'warning',
    activeBorder: 'border-[var(--color-warning)] ring-1 ring-[var(--color-warning)]',
    activeBg: 'bg-[var(--color-warning-soft)]',
  },
  {
    value: 'REJECTED',
    title: 'Reject Admission',
    subtitle: 'Medically unfit or contagious',
    description:
      'Significant medical issues, incurable unsoundness, or contagious pathogen risk detected.',
    outcomeNotes: [
      'Admission application is formally rejected',
      'Horse transitions to REJECTED status',
      'Quarantine stall will be released',
      'Training lock remains permanently locked',
      'Formal medical reason is mandatory',
    ],
    icon: 'x',
    tone: 'danger',
    activeBorder: 'border-[var(--color-danger)] ring-1 ring-[var(--color-danger)]',
    activeBg: 'bg-[var(--color-danger-soft)]',
  },
];

interface MetricConfig {
  key: MetricField;
  label: string;
  unit: string;
  step: string;
  placeholder: string;
  normalMin: number;
  normalMax: number;
  normalLabel: string;
}

const metricConfigs: MetricConfig[] = [
  {
    key: 'temperature',
    label: 'Body Temperature',
    unit: '°C',
    step: '0.1',
    placeholder: '38.0',
    normalMin: 37.2,
    normalMax: 38.3,
    normalLabel: '37.2 – 38.3 °C',
  },
  {
    key: 'heartRate',
    label: 'Heart Rate',
    unit: 'bpm',
    step: '1',
    placeholder: '36',
    normalMin: 28,
    normalMax: 44,
    normalLabel: '28 – 44 bpm',
  },
  {
    key: 'respiratoryRate',
    label: 'Respiratory Rate',
    unit: 'rpm',
    step: '1',
    placeholder: '12',
    normalMin: 8,
    normalMax: 16,
    normalLabel: '8 – 16 rpm',
  },
  {
    key: 'weight',
    label: 'Body Weight',
    unit: 'kg',
    step: '0.5',
    placeholder: '500',
    normalMin: 400,
    normalMax: 600,
    normalLabel: '400 – 600 kg',
  },
  {
    key: 'bodyConditionScore',
    label: 'BCS (Henneke)',
    unit: '/ 9',
    step: '0.5',
    placeholder: '5.0',
    normalMin: 4.0,
    normalMax: 6.0,
    normalLabel: '4.0 – 6.0 (Ideal)',
  },
];

const emptyMetrics: Record<MetricField, string> = {
  heartRate: '',
  temperature: '',
  weight: '',
  respiratoryRate: '',
  hydrationStatus: '',
  bodyConditionScore: '',
  notes: '',
};

function tomorrowIsoDate() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

function getMetricStatus(
  valStr: string,
  min: number,
  max: number,
): { label: string; tone: 'success' | 'warning' | 'danger' } | null {
  if (!valStr.trim()) return null;
  const num = Number(valStr);
  if (Number.isNaN(num)) return null;
  if (num < min) {
    return { label: 'Low', tone: 'warning' };
  }
  if (num > max) {
    return { label: 'Elevated', tone: 'danger' };
  }
  return { label: 'Normal', tone: 'success' };
}

interface VetReviewFormProps {
  admissionId: number;
  candidateName?: string;
  quarantineStallCode?: string | null;
  onSuccess: (result: VetReviewResponse) => void;
}

export function VetReviewForm({
  admissionId,
  candidateName = 'Candidate horse',
  quarantineStallCode,
  onSuccess,
}: VetReviewFormProps) {
  const [decision, setDecision] = useState<VetDecision>('APPROVED');
  const [physicalExamConfirmed, setPhysicalExamConfirmed] = useState(true);
  const [symptoms, setSymptoms] = useState('');
  const [findings, setFindings] = useState('');
  const [diagnosis, setDiagnosis] = useState('');
  const [treatment, setTreatment] = useState('');
  const [notes, setNotes] = useState('');
  const [followUpDate, setFollowUpDate] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [metrics, setMetrics] = useState<Record<MetricField, string>>(emptyMetrics);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const inFlight = useRef(false);

  function validate(): string | null {
    if (!physicalExamConfirmed) {
      return 'Physical examination confirmation is required before submitting review.';
    }
    if (!findings.trim()) {
      return 'Clinical findings are required to complete the physical examination.';
    }
    if (decision === 'RECHECK_REQUIRED') {
      if (!followUpDate) {
        return 'A future follow-up date is required when scheduling a recheck.';
      }
      if (followUpDate < tomorrowIsoDate()) {
        return 'Follow-up date must be at least one day in the future.';
      }
    }
    if (decision === 'REJECTED' && !rejectionReason.trim()) {
      return 'A detailed medical rejection reason is required to reject an admission.';
    }
    return null;
  }

  function handleFormSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }
    setError('');
    setShowConfirm(true);
  }

  async function executeSubmit() {
    if (inFlight.current) return;
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      setShowConfirm(false);
      return;
    }

    const metricValues = Object.fromEntries(
      Object.entries(metrics)
        .filter(([, value]) => value.trim())
        .map(([key, value]) => [
          key,
          ['hydrationStatus', 'notes'].includes(key) ? value.trim() : Number(value),
        ]),
    ) as HorseHealthMetricRequest;

    const request: VetReviewRequest = {
      decision,
      physicalExamConfirmed: true,
      findings: findings.trim(),
      ...(symptoms.trim() && { symptoms: symptoms.trim() }),
      ...(diagnosis.trim() && { diagnosis: diagnosis.trim() }),
      ...(treatment.trim() && { treatment: treatment.trim() }),
      ...(notes.trim() && { notes: notes.trim(), feedback: notes.trim() }),
      ...(decision === 'RECHECK_REQUIRED' && { followUpDate }),
      ...(decision === 'REJECTED' && {
        rejectionReason: rejectionReason.trim(),
        feedback: rejectionReason.trim(),
      }),
      ...(Object.keys(metricValues).length && { metrics: [metricValues] }),
    };

    inFlight.current = true;
    setSubmitting(true);
    setError('');
    try {
      const result = await admissionsApi.vetReview(admissionId, request);
      setShowConfirm(false);
      onSuccess(result);
    } catch (cause) {
      if (cause instanceof ApiError) {
        setError(
          `${cause.status === 403 ? 'Permission denied' : cause.status === 409 ? 'Review state conflict' : 'Review failed'} (${cause.status}${cause.errorCode ? ` · ${cause.errorCode}` : ''}): ${cause.message}`,
        );
      } else {
        setError(cause instanceof Error ? cause.message : 'Failed to submit review.');
      }
      setShowConfirm(false);
    } finally {
      inFlight.current = false;
      setSubmitting(false);
    }
  }

  const inputClass =
    'w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2 text-[13px] text-[var(--color-text-primary)] outline-none transition-colors placeholder:text-[var(--color-text-muted)] focus:border-[var(--color-primary)] focus:ring-1 focus:ring-[var(--color-primary)] disabled:opacity-60';

  const isFormValid = !validate();

  return (
    <>
      <Panel padded className="border-2 border-[var(--color-primary)]">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-[var(--color-border)] pb-4">
          <div>
            <div className="flex items-center gap-2">
              <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
                <Icon name="stethoscope" size={16} />
              </span>
              <SectionTitle>Clinical Examination &amp; Veterinary Decision</SectionTitle>
            </div>
            <p className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
              Record vitals, document physical examination findings, and render the final admission outcome for{' '}
              <strong className="text-[var(--color-text-primary)]">{candidateName}</strong>.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-[var(--color-primary-soft)] px-2.5 py-1 text-[11px] font-semibold text-[var(--color-primary)]">
              <Icon name="shield" size={12} />
              Veterinarian Workspace
            </span>
          </div>
        </div>

        <form onSubmit={handleFormSubmit} className="mt-5 space-y-6">
          {/* Section 1: Vitals & Health Metrics */}
          <fieldset disabled={submitting} className="space-y-3">
            <div className="flex items-center justify-between">
              <legend className="text-[12px] font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">
                1. Patient Vitals &amp; Telemetry
              </legend>
              <span className="text-[11px] text-[var(--color-text-muted)]">
                Standard resting adult equine reference ranges
              </span>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
              {metricConfigs.map((cfg) => {
                const status = getMetricStatus(metrics[cfg.key], cfg.normalMin, cfg.normalMax);
                return (
                  <div
                    key={cfg.key}
                    className="flex flex-col justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3 transition-colors hover:border-[var(--color-border-strong)]"
                  >
                    <div>
                      <div className="flex items-center justify-between gap-1">
                        <label
                          htmlFor={`metric-${cfg.key}`}
                          className="text-[11px] font-semibold text-[var(--color-text-primary)]"
                        >
                          {cfg.label}
                        </label>
                        {status && (
                          <Pill tone={status.tone} size="sm">
                            {status.label}
                          </Pill>
                        )}
                      </div>
                      <span className="mt-0.5 block text-[10px] text-[var(--color-text-muted)]">
                        Normal: {cfg.normalLabel}
                      </span>
                    </div>

                    <div className="relative mt-2">
                      <input
                        id={`metric-${cfg.key}`}
                        type="number"
                        min="0"
                        step={cfg.step}
                        placeholder={cfg.placeholder}
                        className={`${inputClass} pr-10 font-mono text-[13px] font-semibold`}
                        value={metrics[cfg.key]}
                        onChange={(e) =>
                          setMetrics((curr) => ({ ...curr, [cfg.key]: e.target.value }))
                        }
                      />
                      <span className="pointer-events-none absolute right-2.5 top-2 text-[11px] font-medium text-[var(--color-text-muted)]">
                        {cfg.unit}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <label
                  htmlFor="metric-hydration"
                  className="mb-1 block text-[12px] font-medium text-[var(--color-text-secondary)]"
                >
                  Hydration Status &amp; Mucous Membranes
                </label>
                <div className="relative">
                  <input
                    id="metric-hydration"
                    type="text"
                    maxLength={50}
                    className={inputClass}
                    value={metrics.hydrationStatus}
                    onChange={(e) =>
                      setMetrics((curr) => ({ ...curr, hydrationStatus: e.target.value }))
                    }
                    placeholder="e.g., Normal (CRT < 2s, pink), Mild dehydration, Tacky gums…"
                  />
                  <div className="mt-1 flex flex-wrap gap-1.5">
                    {['Normal (CRT < 2s)', 'Mild dehydration', 'Moderate dehydration'].map(
                      (preset) => (
                        <button
                          key={preset}
                          type="button"
                          onClick={() =>
                            setMetrics((curr) => ({ ...curr, hydrationStatus: preset }))
                          }
                          className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-1.5 py-0.5 text-[10px] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
                        >
                          + {preset}
                        </button>
                      ),
                    )}
                  </div>
                </div>
              </div>

              <div>
                <label
                  htmlFor="metric-notes"
                  className="mb-1 block text-[12px] font-medium text-[var(--color-text-secondary)]"
                >
                  Telemetry &amp; Observation Context
                </label>
                <input
                  id="metric-notes"
                  type="text"
                  maxLength={2000}
                  className={inputClass}
                  value={metrics.notes}
                  onChange={(e) => setMetrics((curr) => ({ ...curr, notes: e.target.value }))}
                  placeholder="e.g., Post-transport reading, calm disposition, standing weight"
                />
              </div>
            </div>
          </fieldset>

          {/* Section 2: Clinical Documentation */}
          <fieldset disabled={submitting} className="space-y-4 border-t border-[var(--color-border)] pt-4">
            <div className="flex items-center justify-between">
              <legend className="text-[12px] font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">
                2. Clinical Findings &amp; Diagnostics
              </legend>
              <span className="text-[11px] text-[var(--color-danger)] font-medium">
                * Required fields
              </span>
            </div>

            {/* Mandatory Physical Exam Confirmation Checkbox */}
            <div className="rounded-[var(--radius-md)] border border-[var(--color-primary)] bg-[var(--color-primary-soft)] p-3">
              <label className="flex cursor-pointer items-start gap-2.5">
                <input
                  type="checkbox"
                  checked={physicalExamConfirmed}
                  onChange={(e) => setPhysicalExamConfirmed(e.target.checked)}
                  className="mt-0.5 h-4 w-4 rounded accent-[var(--color-primary)]"
                />
                <div className="text-[12px] leading-5 text-[var(--color-text-primary)]">
                  <span className="font-semibold">
                    Physical Examination Conducted In-Person
                  </span>{' '}
                  <span className="text-[var(--color-danger)] font-bold">*</span>
                  <p className="text-[11px] text-[var(--color-text-secondary)]">
                    I verify that I have completed an in-person physical clinical examination of{' '}
                    <strong>{candidateName}</strong> in quarantine facility (
                    {quarantineStallCode || 'Quarantine Stall'}).
                  </p>
                </div>
              </label>
            </div>

            <div className="grid gap-4 xl:grid-cols-2">
              <div>
                <label
                  htmlFor="field-symptoms"
                  className="mb-1 block text-[12px] font-medium text-[var(--color-text-primary)]"
                >
                  Observed Symptoms &amp; Presenting Complaints
                </label>
                <textarea
                  id="field-symptoms"
                  className={`${inputClass} resize-y`}
                  rows={3}
                  value={symptoms}
                  onChange={(e) => setSymptoms(e.target.value)}
                  placeholder="e.g., Slight nasal discharge, minor heel soreness, normal respiration at rest, no signs of strangles or acute fever..."
                />
              </div>

              <div>
                <label
                  htmlFor="field-findings"
                  className="mb-1 block text-[12px] font-medium text-[var(--color-text-primary)]"
                >
                  Physical Examination Findings <span className="text-[var(--color-danger)] font-bold">*</span>
                </label>
                <textarea
                  id="field-findings"
                  className={`${inputClass} resize-y ${!findings.trim() ? 'border-[var(--color-border-strong)]' : ''}`}
                  rows={3}
                  value={findings}
                  onChange={(e) => {
                    setFindings(e.target.value);
                    if (error) setError('');
                  }}
                  required
                  placeholder="Systematic evaluation: cardiovascular auscultation, airway clarity, gait & flexion tests, hoof integrity, integumentary inspection..."
                />
                {!findings.trim() && (
                  <span className="mt-1 block text-[11px] text-[var(--color-text-muted)]">
                    Findings are mandatory to formulate the clinical health record.
                  </span>
                )}
              </div>

              <div>
                <label
                  htmlFor="field-diagnosis"
                  className="mb-1 block text-[12px] font-medium text-[var(--color-text-primary)]"
                >
                  Clinical Assessment &amp; Diagnosis
                </label>
                <textarea
                  id="field-diagnosis"
                  className={`${inputClass} resize-y`}
                  rows={2}
                  value={diagnosis}
                  onChange={(e) => setDiagnosis(e.target.value)}
                  placeholder="e.g., Healthy equine candidate; no evidence of communicable pathogens or musculoskeletal impediment..."
                />
              </div>

              <div>
                <label
                  htmlFor="field-treatment"
                  className="mb-1 block text-[12px] font-medium text-[var(--color-text-primary)]"
                >
                  Treatment &amp; Care Protocol (if indicated)
                </label>
                <textarea
                  id="field-treatment"
                  className={`${inputClass} resize-y`}
                  rows={2}
                  value={treatment}
                  onChange={(e) => setTreatment(e.target.value)}
                  placeholder="e.g., Preventative deworming scheduled; hoof dressing twice weekly; routine vaccination update..."
                />
              </div>
            </div>

            <div>
              <label
                htmlFor="field-notes"
                className="mb-1 block text-[12px] font-medium text-[var(--color-text-primary)]"
              >
                Veterinarian Notes &amp; Groom Directives
              </label>
              <textarea
                id="field-notes"
                className={`${inputClass} resize-y`}
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Internal clinical comments, special handling instructions for stable staff, or diet recommendations..."
              />
            </div>
          </fieldset>

          {/* Section 3: Final Decision Selection */}
          <fieldset disabled={submitting} className="space-y-4 border-t border-[var(--color-border)] pt-4">
            <div className="flex items-center justify-between">
              <legend className="text-[12px] font-semibold uppercase tracking-wide text-[var(--color-text-secondary)]">
                3. Final Admission Decision
              </legend>
              <span className="text-[11px] text-[var(--color-text-muted)]">
                Select one of three definitive clinical outcomes
              </span>
            </div>

            <div className="grid gap-3 lg:grid-cols-3">
              {decisions.map((opt) => {
                const isSelected = decision === opt.value;
                return (
                  <label
                    key={opt.value}
                    className={`flex cursor-pointer flex-col justify-between rounded-[var(--radius-lg)] border p-4 transition-all ${
                      isSelected
                        ? `${opt.activeBorder} ${opt.activeBg} shadow-sm`
                        : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-border-strong)] hover:bg-[var(--color-surface-subtle)]'
                    }`}
                  >
                    <div>
                      <div className="flex items-center justify-between gap-2">
                        <span className="flex items-center gap-2 text-[14px] font-bold text-[var(--color-text-primary)]">
                          <input
                            type="radio"
                            name="vet-decision"
                            value={opt.value}
                            checked={isSelected}
                            onChange={() => {
                              setDecision(opt.value);
                              setError('');
                            }}
                            className="h-4 w-4 accent-[var(--color-primary)]"
                          />
                          {opt.title}
                        </span>
                        <Pill tone={opt.tone} size="sm" icon={opt.icon}>
                          {opt.value.replace(/_/g, ' ')}
                        </Pill>
                      </div>

                      <p className="mt-1 text-[11px] font-medium text-[var(--color-text-secondary)]">
                        {opt.subtitle}
                      </p>

                      <p className="mt-2 text-[12px] leading-relaxed text-[var(--color-text-secondary)]">
                        {opt.description}
                      </p>
                    </div>

                    <div className="mt-4 border-t border-[var(--color-border)] pt-2 text-[11px] text-[var(--color-text-muted)]">
                      <span className="font-semibold text-[var(--color-text-secondary)]">
                        Workflow impacts:
                      </span>
                      <ul className="mt-1 space-y-0.5 list-disc pl-4">
                        {opt.outcomeNotes.map((note, idx) => (
                          <li key={idx}>{note}</li>
                        ))}
                      </ul>
                    </div>
                  </label>
                );
              })}
            </div>

            {/* Recheck Required Specific Field */}
            {decision === 'RECHECK_REQUIRED' && (
              <div className="rounded-[var(--radius-md)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] p-4">
                <div className="flex items-start gap-3">
                  <Icon
                    name="calendar"
                    size={20}
                    className="mt-0.5 shrink-0 text-[var(--color-warning)]"
                  />
                  <div className="min-w-0 flex-1">
                    <label
                      htmlFor="field-followup-date"
                      className="block text-[13px] font-bold text-[var(--color-text-primary)]"
                    >
                      Mandatory Follow-Up Examination Date{' '}
                      <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <p className="mt-0.5 text-[12px] text-[var(--color-text-secondary)]">
                      Specify the scheduled date for re-evaluating the horse. The horse will stay in quarantine
                      with training locked until cleared on this follow-up date.
                    </p>
                    <div className="mt-3 flex flex-wrap items-center gap-3">
                      <input
                        id="field-followup-date"
                        type="date"
                        min={tomorrowIsoDate()}
                        className={`${inputClass} max-w-xs font-mono font-medium`}
                        value={followUpDate}
                        onChange={(e) => {
                          setFollowUpDate(e.target.value);
                          if (error) setError('');
                        }}
                        required
                        disabled={submitting}
                      />
                      <div className="flex gap-1.5">
                        {[3, 7, 14].map((days) => {
                          const target = new Date();
                          target.setDate(target.getDate() + days);
                          const iso = target.toISOString().slice(0, 10);
                          return (
                            <button
                              key={days}
                              type="button"
                              onClick={() => setFollowUpDate(iso)}
                              className="rounded border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1 text-[11px] font-medium text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]"
                            >
                              +{days} days
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Rejected Specific Field & Warning */}
            {decision === 'REJECTED' && (
              <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-4">
                <div className="flex items-start gap-3">
                  <Icon
                    name="alert-triangle"
                    size={20}
                    className="mt-0.5 shrink-0 text-[var(--color-danger)]"
                  />
                  <div className="min-w-0 flex-1">
                    <label
                      htmlFor="field-rejection-reason"
                      className="block text-[13px] font-bold text-[var(--color-danger)]"
                    >
                      Formal Medical Rejection Reason{' '}
                      <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <p className="mt-0.5 text-[12px] text-[var(--color-text-secondary)]">
                      Critical Action: Document the exact pathology, disqualifying soundness issue, or bio-security
                      hazard. This text will be permanently stored on the horse record and transmitted to the Owner and
                      Racing Manager.
                    </p>
                    <textarea
                      id="field-rejection-reason"
                      className={`${inputClass} mt-2 resize-y border-[var(--color-danger)]`}
                      rows={3}
                      value={rejectionReason}
                      onChange={(e) => {
                        setRejectionReason(e.target.value);
                        if (error) setError('');
                      }}
                      required
                      placeholder="Specify comprehensive medical and regulatory grounds for rejection..."
                      disabled={submitting}
                    />
                  </div>
                </div>
              </div>
            )}
          </fieldset>

          {/* Error Banner */}
          {error && (
            <div
              role="alert"
              className="flex items-start gap-2.5 rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-[13px] text-[var(--color-danger)]"
            >
              <Icon name="alert-triangle" size={16} className="mt-0.5 shrink-0" />
              <span>{error}</span>
            </div>
          )}

          {/* Form Actions Footer */}
          <div className="flex flex-wrap items-center justify-between gap-4 border-t border-[var(--color-border)] pt-5">
            <div className="flex items-center gap-2 text-[12px] text-[var(--color-text-secondary)]">
              <Icon name="lock" size={14} className="text-[var(--color-text-muted)]" />
              <span>
                Completing this review links the health record and updates the veterinary stage of the admission workflow.
              </span>
            </div>

            <div className="flex items-center gap-2">
              <Button
                type="submit"
                variant={decision === 'REJECTED' ? 'destructive' : 'primary'}
                loading={submitting}
                disabled={submitting || !isFormValid}
                icon={
                  decision === 'RECHECK_REQUIRED'
                    ? 'calendar'
                    : decision === 'REJECTED'
                      ? 'x'
                      : 'check'
                }
              >
                {decision === 'APPROVED'
                  ? 'Complete & Approve Vet Review'
                  : decision === 'RECHECK_REQUIRED'
                    ? 'Complete & Require Recheck'
                    : 'Complete & Reject Admission'}
              </Button>
            </div>
          </div>
        </form>
      </Panel>

      {/* Confirmation Dialogs */}
      <ConfirmDialog
        open={showConfirm && decision === 'APPROVED'}
        title="Approve Veterinary Review?"
        description={
          <div className="space-y-2 text-[13px]">
            <p>
              You are certifying that <strong>{candidateName}</strong> has passed veterinary examination with no
              communicable illness or disqualifying pathology.
            </p>
            <div className="rounded-[var(--radius-sm)] bg-[var(--color-success-soft)] p-2.5 text-[12px] text-[var(--color-success)] font-medium">
              ✓ Vet decision will be recorded as APPROVED.
              <br />✓ Admission will move to Trainer review.
              <br />✓ Horse remains a CANDIDATE in {quarantineStallCode || 'Quarantine'} with training locked.
            </div>
          </div>
        }
        confirmLabel="Approve Vet Review"
        cancelLabel="Back to Review"
        tone="primary"
        loading={submitting}
        onConfirm={executeSubmit}
        onCancel={() => setShowConfirm(false)}
      />

      <ConfirmDialog
        open={showConfirm && decision === 'RECHECK_REQUIRED'}
        title="Schedule Veterinary Recheck?"
        description={
          <div className="space-y-2 text-[13px]">
            <p>
              A follow-up recheck examination for <strong>{candidateName}</strong> will be scheduled for{' '}
              <strong>{followUpDate}</strong>.
            </p>
            <div className="rounded-[var(--radius-sm)] bg-[var(--color-warning-soft)] p-2.5 text-[12px] text-[var(--color-warning)] font-medium">
              ⚠ The horse remains in quarantine ({quarantineStallCode || 'Isolation Stall'}).
              <br />⚠ Training remains strictly locked until follow-up clearance.
            </div>
          </div>
        }
        confirmLabel="Confirm Recheck Schedule"
        cancelLabel="Back to Review"
        tone="primary"
        loading={submitting}
        onConfirm={executeSubmit}
        onCancel={() => setShowConfirm(false)}
      />

      <ConfirmDialog
        open={showConfirm && decision === 'REJECTED'}
        title="Confirm Medical Rejection?"
        description={
          <div className="space-y-2 text-[13px]">
            <p className="font-semibold text-[var(--color-danger)]">
              This action cannot be undone through normal intake workflows.
            </p>
            <p>
              <strong>{candidateName}</strong> will be formally rejected for entry on veterinary grounds:
            </p>
            <blockquote className="rounded-[var(--radius-sm)] border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-2.5 text-[12px] italic text-[var(--color-danger)]">
              &quot;{rejectionReason}&quot;
            </blockquote>
            <p className="text-[12px] text-[var(--color-text-muted)]">
              The quarantine stall will be freed, and training will remain permanently locked.
            </p>
          </div>
        }
        confirmLabel="Reject Admission"
        cancelLabel="Cancel"
        tone="danger"
        loading={submitting}
        onConfirm={executeSubmit}
        onCancel={() => setShowConfirm(false)}
      />
    </>
  );
}
