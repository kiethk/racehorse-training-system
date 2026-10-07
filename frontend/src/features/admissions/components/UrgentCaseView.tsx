'use client';

import { useEffect, useState, type FormEvent } from 'react';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '../services/api';
import type {
  CompleteCareScheduleRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  UrgentAssignmentAlert,
} from '../types';

function formatDateTime(value: string | null) {
  if (!value) return 'Unknown';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function UrgentCaseView({ scheduleId }: { scheduleId: number }) {
  const [urgentCase, setUrgentCase] = useState<UrgentAssignmentAlert | null>(null);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [completedData, setCompletedData] = useState<CompleteCareScheduleRequest | null>(null);

  // Form states
  const [symptoms, setSymptoms] = useState('');
  const [findings, setFindings] = useState('');
  const [diagnosis, setDiagnosis] = useState('');
  const [treatment, setTreatment] = useState('');
  const [trainingDecision, setTrainingDecision] = useState<TrainingDecision>('RESTRICTED');
  const [restrictionDetails, setRestrictionDetails] = useState('');
  const [notes, setNotes] = useState('');
  const [temperature, setTemperature] = useState('');
  const [heartRate, setHeartRate] = useState('');
  const [respiratoryRate, setRespiratoryRate] = useState('');
  const [weight, setWeight] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let active = true;
    admissionsApi
      .getUrgentCase(scheduleId)
      .then((data) => {
        if (!active) return;
        setUrgentCase(data);
        if (data.description) {
          setSymptoms((prev) => prev || data.description);
        }
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(cause instanceof ApiError ? cause.message : 'Unable to load the urgent case.');
      })
      .finally(() => active && setLoading(false));
    return () => {
      active = false;
    };
  }, [scheduleId]);

  async function startExam() {
    if (!urgentCase || urgentCase.status !== 'SCHEDULED') return;
    setStarting(true);
    setError('');
    try {
      const schedule = await admissionsApi.startCareSchedule(urgentCase.scheduleId);
      setUrgentCase((current) => (current ? { ...current, status: schedule.status } : current));
    } catch (cause) {
      setError(cause instanceof ApiError ? cause.message : 'Unable to start the examination.');
    } finally {
      setStarting(false);
    }
  }

  function validateForm(): boolean {
    const errors: Record<string, string> = {};

    if (!symptoms.trim()) {
      errors.symptoms = 'Symptoms are required for an urgent examination.';
    }
    if (!findings.trim()) {
      errors.findings = 'Physical findings are required.';
    }
    if (!diagnosis.trim()) {
      errors.diagnosis = 'Clinical diagnosis is required.';
    }
    if (trainingDecision === 'RESTRICTED' || trainingDecision === 'BLOCKED') {
      if (!restrictionDetails.trim()) {
        errors.restrictionDetails = 'Restriction details are required for RESTRICTED or BLOCKED decisions.';
      }
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!urgentCase) return;

    if (!validateForm()) {
      setSubmitError('Please review the required fields.');
      return;
    }

    setSubmitting(true);
    setSubmitError('');

    const metrics: HorseHealthMetricRequest = {};
    if (temperature.trim()) metrics.temperature = Number(temperature);
    if (heartRate.trim()) metrics.heartRate = Number(heartRate);
    if (respiratoryRate.trim()) metrics.respiratoryRate = Number(respiratoryRate);
    if (weight.trim()) metrics.weight = Number(weight);

    const payload: CompleteCareScheduleRequest = {
      symptoms: symptoms.trim(),
      findings: findings.trim(),
      diagnosis: diagnosis.trim(),
      treatment: treatment.trim() || undefined,
      trainingDecision,
      restrictionDetails: restrictionDetails.trim() || undefined,
      notes: notes.trim() || undefined,
      metrics: Object.keys(metrics).length > 0 ? [metrics] : undefined,
    };

    try {
      await admissionsApi.completeCareSchedule(urgentCase.scheduleId, payload);
      setCompletedData(payload);
      setUrgentCase((current) => (current ? { ...current, status: 'COMPLETED' } : current));
    } catch (cause) {
      setSubmitError(
        cause instanceof ApiError
          ? cause.message
          : 'An error occurred while completing the examination. Your input was preserved; please try again.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return <Panel className="p-8 text-sm text-[var(--color-text-secondary)]">Loading urgent case…</Panel>;
  }
  if (!urgentCase) {
    return (
      <Panel className="border-[var(--color-danger)] p-8 text-sm text-[var(--color-danger)]">
        {error || 'Urgent case not found.'}
      </Panel>
    );
  }

  return (
    <div className="mx-auto w-full max-w-4xl space-y-5">
      <Panel className="overflow-hidden border-2 border-[var(--color-danger)] p-0">
        <header className="flex flex-wrap items-start gap-4 border-b border-[var(--color-danger)]/30 bg-[var(--color-danger-soft)] p-6">
          <span className="flex h-12 w-12 items-center justify-center rounded-full bg-[var(--color-danger)] text-white">
            <Icon name="alert-triangle" size={24} />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-[11px] font-bold uppercase tracking-[0.14em] text-[var(--color-danger)]">
              Urgent case #{urgentCase.scheduleId}
            </p>
            <h1 className="mt-1 text-2xl font-bold text-[var(--color-text-primary)]">{urgentCase.title}</h1>
            <p className="mt-1 text-sm text-[var(--color-text-secondary)]">
              You were directly assigned to this case by the system.
            </p>
          </div>
          <div className="flex gap-2">
            <Pill tone="danger">{urgentCase.severity}</Pill>
            <Pill tone={urgentCase.status === 'COMPLETED' ? 'success' : 'info'}>{urgentCase.status}</Pill>
          </div>
        </header>

        <div className="space-y-5 p-6">
          <dl className="grid gap-4 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Horse</dt>
              <dd className="mt-1 font-bold">
                {urgentCase.horseName} · #{urgentCase.horseId}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Stall / location</dt>
              <dd className="mt-1 font-semibold">
                {[urgentCase.stallCode, urgentCase.stableLocation].filter(Boolean).join(' · ') || 'Not updated'}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Reported by</dt>
              <dd className="mt-1 font-semibold">
                {urgentCase.reportedByName || 'Unknown'} · #{urgentCase.reportedById}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Reported at</dt>
              <dd className="mt-1 font-semibold">{formatDateTime(urgentCase.reportedAt)}</dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">
                Assigned at
              </dt>
              <dd className="mt-1 font-semibold">{formatDateTime(urgentCase.assignedAt)}</dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase text-[var(--color-text-muted)]">Training status</dt>
              <dd className="mt-1 font-bold text-[var(--color-danger)]">
                {urgentCase.trainingStatus} — Training blocked
              </dd>
            </div>
          </dl>

          <section className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-4">
            <h2 className="text-sm font-bold text-[var(--color-text-primary)]">Symptoms / initial description</h2>
            <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-[var(--color-text-secondary)]">
              {urgentCase.description}
            </p>
          </section>

          {urgentCase.imageUrl && (
            <a
              href={admissionsApi.assetUrl(urgentCase.imageUrl)}
              target="_blank"
              rel="noreferrer"
              className="block overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)]"
            >
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src={admissionsApi.assetUrl(urgentCase.imageUrl)}
                alt="Urgent case report"
                className="max-h-[28rem] w-full object-contain"
              />
            </a>
          )}

          {error && (
            <p role="alert" className="text-sm font-medium text-[var(--color-danger)]">
              {error}
            </p>
          )}

          {/* Workflow Transitions */}
          {urgentCase.status === 'SCHEDULED' && (
            <div className="pt-2">
              <Button variant="destructive" icon="activity" disabled={starting} onClick={startExam}>
                {starting ? 'Starting…' : 'Start examination'}
              </Button>
            </div>
          )}

          {urgentCase.status === 'IN_PROGRESS' && (
            <form onSubmit={handleSubmit} className="space-y-5 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
              <div className="border-b border-[var(--color-border)] pb-3">
                <h3 className="text-lg font-bold text-[var(--color-text-primary)]">
                  Urgent examination form
                </h3>
                <p className="text-xs text-[var(--color-text-secondary)]">
                  Enter the clinical assessment and medical training decision for this urgent case.
                </p>
              </div>

              {submitError && (
                <div role="alert" className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] p-3 text-sm text-[var(--color-danger)]">
                  {submitError}
                </div>
              )}

              {/* Vitals Telemetry */}
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)] mb-2">
                  1. Clinical vitals
                </label>
                <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                  <div>
                    <label htmlFor="vital-temp" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Temperature (°C)
                    </label>
                    <input
                      id="vital-temp"
                      type="number"
                      step="0.1"
                      placeholder="38.0"
                      value={temperature}
                      onChange={(e) => setTemperature(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                  <div>
                    <label htmlFor="vital-hr" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Heart rate (bpm)
                    </label>
                    <input
                      id="vital-hr"
                      type="number"
                      step="1"
                      placeholder="36"
                      value={heartRate}
                      onChange={(e) => setHeartRate(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                  <div>
                    <label htmlFor="vital-rr" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Respiratory rate (rpm)
                    </label>
                    <input
                      id="vital-rr"
                      type="number"
                      step="1"
                      placeholder="12"
                      value={respiratoryRate}
                      onChange={(e) => setRespiratoryRate(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                  <div>
                    <label htmlFor="vital-wt" className="block text-xs font-medium text-[var(--color-text-primary)]">
                      Weight (kg)
                    </label>
                    <input
                      id="vital-wt"
                      type="number"
                      step="0.5"
                      placeholder="500"
                      value={weight}
                      onChange={(e) => setWeight(e.target.value)}
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                </div>
              </div>

              {/* Clinical Details */}
              <div className="space-y-4">
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  2. Examination and diagnosis
                </label>

                <div>
                  <label htmlFor="urgent-symptoms" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Observed urgent symptoms <span className="text-[var(--color-danger)]">*</span>
                  </label>
                  <textarea
                    id="urgent-symptoms"
                    rows={2}
                    value={symptoms}
                    onChange={(e) => {
                      setSymptoms(e.target.value);
                      if (fieldErrors.symptoms) setFieldErrors((prev) => ({ ...prev, symptoms: '' }));
                    }}
                      placeholder="Describe the horse's symptoms, illness or injury..."
                    className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:border-[var(--color-primary)]"
                  />
                  {fieldErrors.symptoms && (
                    <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.symptoms}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="urgent-findings" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Physical findings <span className="text-[var(--color-danger)]">*</span>
                  </label>
                  <textarea
                    id="urgent-findings"
                    rows={3}
                    value={findings}
                    onChange={(e) => {
                      setFindings(e.target.value);
                      if (fieldErrors.findings) setFieldErrors((prev) => ({ ...prev, findings: '' }));
                    }}
                      placeholder="Record the physical condition, injuries or pain locations..."
                    className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:border-[var(--color-primary)]"
                  />
                  {fieldErrors.findings && (
                    <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.findings}</p>
                  )}
                </div>

                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <label htmlFor="urgent-diagnosis" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                      Diagnosis <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <input
                      id="urgent-diagnosis"
                      type="text"
                      value={diagnosis}
                      onChange={(e) => {
                        setDiagnosis(e.target.value);
                        if (fieldErrors.diagnosis) setFieldErrors((prev) => ({ ...prev, diagnosis: '' }));
                      }}
                      placeholder="Confirmed or provisional diagnosis..."
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                    {fieldErrors.diagnosis && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.diagnosis}</p>
                    )}
                  </div>

                  <div>
                    <label htmlFor="urgent-treatment" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Treatment / first aid
                    </label>
                    <input
                      id="urgent-treatment"
                      type="text"
                      value={treatment}
                      onChange={(e) => setTreatment(e.target.value)}
                    placeholder="Medication, bandaging or care instructions..."
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] px-2.5 py-1.5 text-xs outline-none focus:border-[var(--color-primary)]"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="urgent-notes" className="block text-xs font-semibold text-[var(--color-text-primary)]">
                    Additional notes
                  </label>
                  <textarea
                    id="urgent-notes"
                    rows={2}
                    value={notes}
                    onChange={(e) => setNotes(e.target.value)}
                    placeholder="Care notes or follow-up instructions..."
                    className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:border-[var(--color-primary)]"
                  />
                </div>
              </div>

              {/* Training Clearance Decision */}
              <div className="space-y-3 border-t border-[var(--color-border)] pt-4">
                <label className="block text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  3. Training decision <span className="text-[var(--color-danger)]">*</span>
                </label>
                <div className="grid gap-3 sm:grid-cols-3">
                  {[
                    { value: 'ALLOWED' as const, label: 'ALLOWED', desc: 'Normal training permitted' },
                    { value: 'RESTRICTED' as const, label: 'RESTRICTED', desc: 'Movement restricted with conditions' },
                    { value: 'BLOCKED' as const, label: 'BLOCKED', desc: 'Training prohibited; stall rest required' },
                  ].map((option) => (
                    <label
                      key={option.value}
                      className={`flex cursor-pointer flex-col rounded-[var(--radius-md)] border p-3 transition-colors ${
                        trainingDecision === option.value
                          ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                          : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:bg-[var(--color-surface-muted)]'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <input
                          type="radio"
                          name="trainingDecision"
                          value={option.value}
                          checked={trainingDecision === option.value}
                          onChange={() => {
                            setTrainingDecision(option.value);
                            if (option.value === 'ALLOWED') {
                              setFieldErrors((prev) => ({ ...prev, restrictionDetails: '' }));
                            }
                          }}
                          className="accent-[var(--color-primary)]"
                        />
                        <span className="text-xs font-bold">{option.label}</span>
                      </div>
                      <p className="mt-1 text-[11px] text-[var(--color-text-secondary)]">{option.desc}</p>
                    </label>
                  ))}
                </div>

                {(trainingDecision === 'RESTRICTED' || trainingDecision === 'BLOCKED') && (
                  <div className="mt-3">
                    <label htmlFor="restriction-details" className="block text-xs font-semibold text-[var(--color-danger)]">
                      Movement restriction details <span className="text-[var(--color-danger)]">*</span>
                    </label>
                    <textarea
                      id="restriction-details"
                      rows={2}
                      value={restrictionDetails}
                      onChange={(e) => {
                        setRestrictionDetails(e.target.value);
                        if (fieldErrors.restrictionDetails) {
                          setFieldErrors((prev) => ({ ...prev, restrictionDetails: '' }));
                        }
                      }}
                      placeholder="Describe the restriction (for example, 15 minutes of walking only or complete stall rest)..."
                      className="mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-surface)] p-2.5 text-xs outline-none focus:ring-1 focus:ring-[var(--color-danger)]"
                    />
                    {fieldErrors.restrictionDetails && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.restrictionDetails}</p>
                    )}
                  </div>
                )}
              </div>

              <div className="border-t border-[var(--color-border)] pt-4">
                <Button type="submit" variant="primary" loading={submitting} icon="check" className="w-full sm:w-auto">
                  Complete urgent examination
                </Button>
              </div>
            </form>
          )}

          {urgentCase.status === 'COMPLETED' && (
            <div className="space-y-4 rounded-[var(--radius-md)] border border-[var(--color-success)] bg-[var(--color-success-soft)] p-6">
              <div className="flex items-center gap-3 text-[var(--color-success)]">
                <Icon name="check" size={24} />
                <h3 className="text-base font-bold">Urgent examination completed successfully.</h3>
              </div>
              <p className="text-xs text-[var(--color-text-secondary)]">
                The clinical record and training decision have been saved.
              </p>
              {completedData && (
                <dl className="grid gap-3 rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-xs sm:grid-cols-2">
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Diagnosis</dt>
                    <dd className="mt-1 font-bold text-[var(--color-text-primary)]">{completedData.diagnosis}</dd>
                  </div>
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Training decision</dt>
                    <dd className="mt-1 font-bold text-[var(--color-danger)]">{completedData.trainingDecision}</dd>
                  </div>
                  <div className="sm:col-span-2">
                    <dt className="font-semibold text-[var(--color-text-muted)]">Examination findings</dt>
                    <dd className="mt-1 text-[var(--color-text-secondary)]">{completedData.findings}</dd>
                  </div>
                  {completedData.restrictionDetails && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Restriction details</dt>
                      <dd className="mt-1 text-[var(--color-danger)]">{completedData.restrictionDetails}</dd>
                    </div>
                  )}
                </dl>
              )}
            </div>
          )}
        </div>
      </Panel>
    </div>
  );
}
