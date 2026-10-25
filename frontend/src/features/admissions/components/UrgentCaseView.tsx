'use client';

import React, { useEffect, useState, type FormEvent } from 'react';
import { z } from 'zod';
import dynamic from 'next/dynamic';
import Link from 'next/link';
import { Notice } from '@/components/ui/Notice';
import { ChoiceInput, Input, Textarea } from '@/components/ui/Input';
import { FormField } from '@/components/ui/FormField';
import { ApiError } from '@/services/api';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { ProtectedImage } from '@/components/ui/ProtectedImage';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { admissionsApi } from '../services/api';
import { healthApi } from '@/features/health/services/healthApi';

const UrgentHorseInjuryViewer = dynamic(
  () => import('@/features/vet/components/UrgentHorseInjuryViewer'),
  { ssr: false },
);
import { careService } from '@/features/care/services/careService';
import { displayError } from '@/lib/display';
import type {
  CompleteCareScheduleRequest,
  HorseHealthMetricRequest,
  TrainingDecision,
  UrgentAssignmentAlert,
  UrgentCaseReport,
} from '../types';

/** Date in Vietnam time, plus dayOffset days, as yyyy-MM-dd (matches the backend). */
function businessDate(dayOffset: number) {
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date());
  const [year, month, day] = today.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day + dayOffset)).toISOString().slice(0, 10);
}

function newIdempotencyKey() {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `follow-up-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function formatDateTime(value: string | null) {
  if (!value) return 'Not specified';
  const date = new Date(value);
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(date);
}

export function UrgentCaseView({ scheduleId }: { scheduleId: number }) {
  const [urgentCase, setUrgentCase] = useState<UrgentAssignmentAlert | null>(null);
  const [reports, setReports] = useState<UrgentCaseReport[]>([]);
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
  const [trainingDecision, setTrainingDecision] = useState<TrainingDecision>('BLOCKED');
  const [restrictionDetails, setRestrictionDetails] = useState('');
  // Blocking training requires a follow-up exam — "rest until" is that exam's date.
  const [followUpDate, setFollowUpDate] = useState(() => businessDate(3));
  const [followUpDescription, setFollowUpDescription] = useState('Follow-up after the urgent examination');
  const [followUpKey, setFollowUpKey] = useState(newIdempotencyKey);
  const [notes, setNotes] = useState('');
  const [temperature, setTemperature] = useState('');
  const [heartRate, setHeartRate] = useState('');
  const [respiratoryRate, setRespiratoryRate] = useState('');
  const [weight, setWeight] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  type InjuryMarker = {
    id: string;
    position: [number, number, number];
    normal: [number, number, number];
    zoneId?: string;
    label: string;
  };

  const [injuryMarkers, setInjuryMarkers] = useState<InjuryMarker[]>([]);
  const [isAddMode, setIsAddMode] = useState(false);
  const [anatomyTab, setAnatomyTab] = useState<'form' | 'anatomy3d'>('form');

  const handleInjuryHit = (hit: {
    position: [number, number, number];
    normal: [number, number, number];
    zoneId: string;
  }) => {
    const id = typeof crypto !== 'undefined' && 'randomUUID' in crypto ? crypto.randomUUID() : `marker-${Date.now()}`;
    setInjuryMarkers((prev) => [
      ...prev,
      { id, position: hit.position, normal: hit.normal, zoneId: hit.zoneId, label: `Marker ${prev.length + 1}` },
    ]);
    setFindings((prev) => {
      const line = `- 3D injury marker at [${hit.position.map((n) => n.toFixed(2)).join(', ')}]`;
      return prev ? `${prev}\n${line}` : line;
    });
  };

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- reset on id change
    setUrgentCase(null);
    setLoading(true);
    setReports([]);
    setError('');
    setSubmitError('');
    setCompletedData(null);
    setStarting(false);
    setSubmitting(false);
    setSymptoms('');
    setFindings('');
    setDiagnosis('');
    setTreatment('');
    setTrainingDecision('BLOCKED');
    setRestrictionDetails('');
    setFollowUpDate(businessDate(3));
    setFollowUpDescription('Follow-up after the urgent examination');
    setFollowUpKey(newIdempotencyKey());
    setNotes('');
    setTemperature('');
    setHeartRate('');
    setRespiratoryRate('');
    setWeight('');
    setFieldErrors({});
    setInjuryMarkers([]);
    setIsAddMode(false);
    setAnatomyTab('form');

    admissionsApi
      .getUrgentCase(scheduleId)
      .then((data) => {
        if (!active) return;
        setUrgentCase(data);
        if (data.description) {
          setSymptoms((prev) => prev || data.description);
        }

        // Load active injury markers (persisted) for read-only display on the 3D model.
        healthApi
          .getActiveInjuries(data.horseId)
          .then((injuries) => {
            if (!active) return;
            setInjuryMarkers(
              injuries.map((inj, idx) => ({
                id: String(inj.id),
                position: [
                  Number(inj.positionX ?? 0),
                  Number(inj.positionY ?? 0),
                  Number(inj.positionZ ?? 0),
                ] as [number, number, number],
                normal: [
                  Number(inj.normalX ?? 0),
                  Number(inj.normalY ?? 1),
                  Number(inj.normalZ ?? 0),
                ] as [number, number, number],
                zoneId: inj.zoneId ?? undefined,
                label: inj.zoneId ? `Injury ${idx + 1}` : `Marker ${idx + 1}`,
              })),
            );
          })
          .catch(() => {
            // Keep empty marker list if active injuries fail to load
          });

        if (data.status === 'COMPLETED') {
          careService
            .getScheduleDetail(scheduleId)
            .then((detail) => {
              if (!active) return;
              if (detail && detail.healthRecord) {
                setCompletedData({
                  symptoms: data.description || '',
                  findings: detail.healthRecord.findings || '',
                  diagnosis: detail.healthRecord.diagnosis || '',
                  treatment: detail.healthRecord.treatment || undefined,
                  trainingDecision: detail.healthRecord.trainingDecision === 'RESTRICTED' ? 'BLOCKED' : (detail.healthRecord.trainingDecision as TrainingDecision),
                  restrictionDetails: detail.healthRecord.restrictionDetails || undefined,
                });
              }
            })
            .catch(() => {});
        }

        admissionsApi
          .getUrgentCaseReports(scheduleId)
          .then((reportList) => {
            if (!active) return;
            setReports(reportList);
          })
          .catch(() => {
            // Keep empty report list if secondary reports fail
          });
      })
      .catch((cause: unknown) => {
        if (!active) return;
        setError(
          cause instanceof ApiError
            ? displayError(cause, 'Unable to load the urgent case.')
            : 'Unable to load the urgent case.',
        );
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
      setError(
        cause instanceof ApiError
          ? displayError(cause, 'Unable to start the examination.')
          : 'Unable to start the examination.',
      );
    } finally {
      setStarting(false);
    }
  }

  function validateForm(): boolean {
    const schema = z
      .object({
        symptoms: z.string().min(1, 'Symptoms are required for an urgent examination.'),
        findings: z.string().min(1, 'Examination findings are required.'),
        diagnosis: z.string().min(1, 'A clinical diagnosis is required.'),
        restrictionDetails: z.string().optional(),
        followUpDate: z.string().optional(),
        followUpDescription: z.string().optional(),
      })
      .superRefine((data, ctx) => {
        if (trainingDecision === 'BLOCKED') {
          if (!data.restrictionDetails?.trim()) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['restrictionDetails'], message: 'Restriction details are required when training is blocked.' });
          }
          if (!data.followUpDate) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['followUpDate'], message: 'Select a follow-up date when training is blocked.' });
          } else if (data.followUpDate < businessDate(1)) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['followUpDate'], message: 'The follow-up date must be tomorrow or later.' });
          }
          if (!data.followUpDescription?.trim()) {
            ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['followUpDescription'], message: 'A follow-up description is required.' });
          }
        }
      });

    const parsed = schema.safeParse({
      symptoms: symptoms.trim(),
      findings: findings.trim(),
      diagnosis: diagnosis.trim(),
      restrictionDetails: restrictionDetails.trim(),
      followUpDate,
      followUpDescription: followUpDescription.trim(),
    });

    if (parsed.success) {
      setFieldErrors({});
      return true;
    }
    const errors: Record<string, string> = {};
    for (const issue of parsed.error.issues) {
      const field = issue.path[0];
      if (typeof field === 'string' && !errors[field]) errors[field] = issue.message;
    }
    setFieldErrors(errors);
    return false;
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    if (!urgentCase) return;

    if (!validateForm()) {
      setSubmitError('Check the required fields and try again.');
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
      injuryMarkers:
        injuryMarkers.length > 0
          ? injuryMarkers.map((m) => ({
              injuryType: 'UNSPECIFIED',
              bodyRegionId: 1,
              severity: 'MODERATE',
              description: m.label,
              diagnosedAt: new Date().toISOString(),
              positionX: m.position[0],
              positionY: m.position[1],
              positionZ: m.position[2],
              zoneId: m.zoneId ?? null,
              localX: null,
              localY: null,
              localZ: null,
              normalX: m.normal[0],
              normalY: m.normal[1],
              normalZ: m.normal[2],
              meshName: 'Horse.001_M_Body_0',
            }))
          : undefined,
      nextSchedule:
        trainingDecision === 'BLOCKED'
          ? {
              horseId: urgentCase.horseId,
              careType: 'ROUTINE',
              scheduledDate: followUpDate,
              description: followUpDescription.trim(),
              idempotencyKey: followUpKey,
            }
          : undefined,
    };

    try {
      await admissionsApi.completeCareSchedule(urgentCase.scheduleId, payload);
      setCompletedData(payload);
      setUrgentCase((current) => (current ? { ...current, status: 'COMPLETED' } : current));
    } catch (cause) {
      setSubmitError(
        cause instanceof ApiError
          ? cause.message
          : 'Unable to complete the examination. Your entries have been kept; please try again.',
      );
    } finally {
      setSubmitting(false);
    }
  }

  if (loading) {
    return (
      <Panel className="p-8 text-center text-sm text-[var(--color-text-secondary)]">
        <Icon name="refresh" size={20} className="animate-spin inline-block mr-2" />
        Loading urgent case…
      </Panel>
    );
  }

  if (!urgentCase) {
    return (
      <div className="space-y-4">
        <Link
          href="/veterinarian/urgent"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--color-primary)] hover:underline"
        >
          <Icon name="arrow-left" size={13} />
          Back to Urgent Cases
        </Link>
        <Notice tone="error">{error || 'Urgent case not found.'}</Notice>
      </div>
    );
  }

  const additionalReports = reports.filter((report) => report.id !== urgentCase.incidentId);

  return (
    <div className="space-y-5">
      {/* Top Breadcrumb Navigation */}
      <div>
        <Link
          href="/veterinarian/urgent"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--color-primary)] hover:underline"
        >
          <Icon name="arrow-left" size={13} />
          Back to Urgent Cases
        </Link>
      </div>

      {/* Clinical Case Header */}
      <Panel className="overflow-hidden border border-[var(--color-border)] p-5 sm:p-6 border-l-4 border-l-[var(--color-medical)] bg-[var(--color-surface)]">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-start gap-4">
            <HorseAvatar name={urgentCase.horseName} image={null} size={56} rounded="md" />
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className="text-xl font-bold text-[var(--color-text-primary)]">
                  {urgentCase.horseName}
                </span>
                <span className="text-xs font-mono text-[var(--color-text-muted)] bg-[var(--color-surface-muted)] px-1.5 py-0.5 rounded-[var(--radius-xs)] border border-[var(--color-border)]">
                  #{urgentCase.horseId}
                </span>
                <Pill tone={urgentCase.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                  {urgentCase.severity}
                </Pill>
                <Pill tone={urgentCase.status === 'COMPLETED' ? 'success' : urgentCase.status === 'IN_PROGRESS' ? 'info' : 'warning'}>
                  {urgentCase.status === 'SCHEDULED' ? 'Pending Intake' : urgentCase.status === 'IN_PROGRESS' ? 'In Progress' : 'Completed'}
                </Pill>
              </div>

              <h1 className="mt-1 text-base font-semibold text-[var(--color-text-primary)]">
                {urgentCase.title}
              </h1>

              <div className="mt-1.5 flex flex-wrap items-center gap-y-1 gap-x-4 text-xs text-[var(--color-text-muted)]">
                {urgentCase.stallCode && (
                  <span className="flex items-center gap-1 font-medium text-[var(--color-text-secondary)]">
                    <Icon name="building" size={12} />
                    <span>Stall {urgentCase.stallCode}</span>
                    {urgentCase.stableLocation && (
                      <span className="text-[var(--color-text-muted)]">({urgentCase.stableLocation})</span>
                    )}
                  </span>
                )}
                <span className="flex items-center gap-1">
                  <Icon name="clock" size={12} />
                  <span>Reported: {formatDateTime(urgentCase.reportedAt)}</span>
                </span>
                <span className="font-mono text-[11px]">
                  Case #{urgentCase.scheduleId}
                </span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 self-start sm:self-center">
            {urgentCase.status === 'SCHEDULED' && (
              <Button
                variant="destructive"
                icon="activity"
                loading={starting}
                onClick={startExam}
                className="transition-all active:scale-[0.97]"
              >
                Start Examination
              </Button>
            )}
          </div>
        </div>
      </Panel>

      {/* 2-Column Responsive Clinical Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Main Column (8 / 12) */}
        <div className="lg:col-span-8 space-y-6">
          {/* Incident Observation & Evidence */}
          <Panel padded className="space-y-4">
            <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-3">
              <div className="flex items-center gap-2">
                <Icon name="file-text" size={16} className="text-[var(--color-medical)]" />
                <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                  Reported Incident & Observation
                </h2>
              </div>
              <span className="text-xs text-[var(--color-text-muted)]">
                Reported by {urgentCase.reportedByName || 'Staff'}
              </span>
            </div>

            <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-[var(--color-text-secondary)]">
              {urgentCase.description || 'No detailed clinical description provided by reporter.'}
            </p>

            {urgentCase.imageUrl && (
              <div className="pt-2">
                <label className="block text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)] mb-2">
                  Photo Evidence
                </label>
                <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2">
                  <ProtectedImage
                    src={urgentCase.imageUrl}
                    alt="Urgent case report incident photo"
                    openOnClick
                    className="max-h-72 w-full object-contain rounded-[var(--radius-sm)]"
                  />
                  <p className="mt-1.5 text-center text-[11px] text-[var(--color-text-muted)]">
                    Click photo to open full resolution view
                  </p>
                </div>
              </div>
            )}
          </Panel>

          {/* Merged Secondary Reports (if any) */}
          {additionalReports.length > 0 && (
            <Panel padded className="space-y-3">
              <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                Additional reports merged into this case ({additionalReports.length})
              </h2>
              <ul className="space-y-3">
                {additionalReports.map((report) => (
                  <li
                    key={report.id}
                    className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5 space-y-2"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <strong className="text-sm text-[var(--color-text-primary)]">{report.title}</strong>
                      <Pill tone="danger">{report.severity}</Pill>
                    </div>
                    <p className="whitespace-pre-wrap break-words text-xs leading-relaxed text-[var(--color-text-secondary)]">
                      {report.description}
                    </p>
                    <div className="flex items-center gap-2 text-[11px] text-[var(--color-text-muted)]">
                      <span>
                        {[report.reportedByRole, report.reportedByName].filter(Boolean).join(' ') ||
                          `#${report.reportedById}`}
                      </span>
                      <span>·</span>
                      <span>{formatDateTime(report.reportedAt)}</span>
                    </div>
                    {report.imageUrl && (
                      <ProtectedImage
                        src={report.imageUrl}
                        alt={`Report: ${report.title}`}
                        openOnClick
                        className="max-h-48 w-full object-contain rounded-[var(--radius-sm)]"
                      />
                    )}
                  </li>
                ))}
              </ul>
            </Panel>
          )}

          {/* Workflow: Awaiting Intake Callout */}
          {urgentCase.status === 'SCHEDULED' && (
            <Panel padded className="border-l-4 border-l-[var(--color-warning)] bg-[var(--color-warning-soft)]/20">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="space-y-1">
                  <h3 className="text-sm font-bold text-[var(--color-text-primary)]">
                    Awaiting Clinical Intake
                  </h3>
                  <p className="text-xs text-[var(--color-text-secondary)]">
                    This urgent case is currently scheduled and assigned to you. Click Start Examination to begin recording clinical findings.
                  </p>
                </div>
                <Button
                  variant="destructive"
                  icon="activity"
                  loading={starting}
                  onClick={startExam}
                  className="transition-all active:scale-[0.97]"
                >
                  Start Examination
                </Button>
              </div>
            </Panel>
          )}

          {/* Workflow: In Progress Examination Form */}
          {urgentCase.status === 'IN_PROGRESS' && (
            <Panel padded className="space-y-6">
              <div className="border-b border-[var(--color-border)] pb-3">
                <div className="flex flex-wrap items-center gap-2">
                  <Button
                    type="button"
                    variant={anatomyTab === 'form' ? 'primary' : 'secondary'}
                    size="sm"
                    onClick={() => setAnatomyTab('form')}
                  >
                    Examination Form
                  </Button>
                  <Button
                    type="button"
                    variant={anatomyTab === 'anatomy3d' ? 'primary' : 'secondary'}
                    size="sm"
                    onClick={() => setAnatomyTab('anatomy3d')}
                  >
                    3D Anatomy
                  </Button>
                </div>
                {anatomyTab === 'form' ? (
                  <p className="mt-2 text-xs text-[var(--color-text-secondary)]">
                    Record vitals telemetry, clinical diagnosis, and immediate training restriction decision.
                  </p>
                ) : (
                  <p className="mt-2 text-xs text-[var(--color-text-secondary)]">
                    Click the horse to place injury markers. Markers are appended to examination findings.
                  </p>
                )}
              </div>

              {submitError && anatomyTab === 'form' && <Notice tone="error">{submitError}</Notice>}

              {anatomyTab === 'anatomy3d' ? (
                <div className="space-y-4">
                  {urgentCase.status === 'IN_PROGRESS' && (
                    <Button
                      type="button"
                      variant={isAddMode ? 'primary' : 'secondary'}
                      onClick={() => setIsAddMode((prev) => !prev)}
                    >
                      {isAddMode ? 'Adding marker: click horse' : 'Add injury marker'}
                    </Button>
                  )}
                  <UrgentHorseInjuryViewer
                    injuries={injuryMarkers}
                    isAddMode={isAddMode}
                    readOnly={urgentCase.status !== 'IN_PROGRESS'}
                    onPointerHit={handleInjuryHit}
                  />
                </div>
              ) : (
              <form onSubmit={handleSubmit} className="space-y-6">
                {/* 1. Clinical Vitals (Integrated Labels, No Bottom Helper Text, No Out-of-Range Warnings) */}
                <div className="space-y-3">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                    <Icon name="heart-pulse" size={14} className="text-[var(--color-medical)]" />
                    <span>1. Clinical Vitals Telemetry</span>
                  </div>

                  <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
                    {/* Temperature */}
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5 transition-colors focus-within:border-[var(--color-primary)] focus-within:bg-[var(--color-surface)]">
                      <label
                        htmlFor="vital-temp"
                        className="block text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]"
                      >
                        Temperature (°C)
                      </label>
                      <Input
                        id="vital-temp"
                        type="number"
                        step="0.1"
                        placeholder="38.0"
                        value={temperature}
                        onChange={(e) => setTemperature(e.target.value)}
                        className="mt-1 w-full !min-h-0 !rounded-none !border-0 !bg-transparent !px-0 !py-0 font-metric text-base font-semibold text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] placeholder:font-normal"
                      />
                    </div>

                    {/* Heart Rate */}
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5 transition-colors focus-within:border-[var(--color-primary)] focus-within:bg-[var(--color-surface)]">
                      <label
                        htmlFor="vital-hr"
                        className="block text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]"
                      >
                        Heart Rate (bpm)
                      </label>
                      <Input
                        id="vital-hr"
                        type="number"
                        step="1"
                        placeholder="36"
                        value={heartRate}
                        onChange={(e) => setHeartRate(e.target.value)}
                        className="mt-1 w-full !min-h-0 !rounded-none !border-0 !bg-transparent !px-0 !py-0 font-metric text-base font-semibold text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] placeholder:font-normal"
                      />
                    </div>

                    {/* Respiratory Rate */}
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5 transition-colors focus-within:border-[var(--color-primary)] focus-within:bg-[var(--color-surface)]">
                      <label
                        htmlFor="vital-rr"
                        className="block text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]"
                      >
                        Respiration (rpm)
                      </label>
                      <Input
                        id="vital-rr"
                        type="number"
                        step="1"
                        placeholder="12"
                        value={respiratoryRate}
                        onChange={(e) => setRespiratoryRate(e.target.value)}
                        className="mt-1 w-full !min-h-0 !rounded-none !border-0 !bg-transparent !px-0 !py-0 font-metric text-base font-semibold text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] placeholder:font-normal"
                      />
                    </div>

                    {/* Weight */}
                    <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5 transition-colors focus-within:border-[var(--color-primary)] focus-within:bg-[var(--color-surface)]">
                      <label
                        htmlFor="vital-wt"
                        className="block text-[11px] font-bold uppercase tracking-wider text-[var(--color-text-muted)]"
                      >
                        Weight (kg)
                      </label>
                      <Input
                        id="vital-wt"
                        type="number"
                        step="0.5"
                        placeholder="500"
                        value={weight}
                        onChange={(e) => setWeight(e.target.value)}
                        className="mt-1 w-full !min-h-0 !rounded-none !border-0 !bg-transparent !px-0 !py-0 font-metric text-base font-semibold text-[var(--color-text-primary)] outline-none placeholder:text-[var(--color-text-muted)] placeholder:font-normal"
                      />
                    </div>
                  </div>
                </div>

                {/* 2. Clinical Findings & Diagnosis */}
                <div className="space-y-4 border-t border-[var(--color-border)] pt-5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                    <Icon name="stethoscope" size={14} className="text-[var(--color-medical)]" />
                    <span>2. Examination Findings & Clinical Diagnosis</span>
                  </div>

                  <div>
                    <FormField label="Observed urgent symptoms" htmlFor="urgent-symptoms" required>
                      <Textarea
                        id="urgent-symptoms"
                        rows={2}
                        value={symptoms}
                        onChange={(e) => {
                          setSymptoms(e.target.value);
                          if (fieldErrors.symptoms) setFieldErrors((prev) => ({ ...prev, symptoms: '' }));
                        }}
                        placeholder="Describe the horse's symptoms or signs of distress…"
                      />
                    </FormField>
                    {fieldErrors.symptoms && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.symptoms}</p>
                    )}
                  </div>

                  <div>
                    <FormField label="Physical examination findings" htmlFor="urgent-findings" required>
                      <Textarea
                        id="urgent-findings"
                        rows={3}
                        value={findings}
                        onChange={(e) => {
                          setFindings(e.target.value);
                          if (fieldErrors.findings) setFieldErrors((prev) => ({ ...prev, findings: '' }));
                        }}
                        placeholder="Detail physical inspection: palpation, limb flexion, swellings, wounds, pain responses…"
                      />
                    </FormField>
                    {fieldErrors.findings && (
                      <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.findings}</p>
                    )}
                  </div>

                  <div className="grid gap-4 sm:grid-cols-2">
                    <div>
                      <FormField label="Clinical diagnosis" htmlFor="urgent-diagnosis" required>
                        <Input
                          id="urgent-diagnosis"
                          type="text"
                          value={diagnosis}
                          onChange={(e) => {
                            setDiagnosis(e.target.value);
                            if (fieldErrors.diagnosis) setFieldErrors((prev) => ({ ...prev, diagnosis: '' }));
                          }}
                          placeholder="e.g. Acute superficial digital flexor tendon strain"
                        />
                      </FormField>
                      {fieldErrors.diagnosis && (
                        <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.diagnosis}</p>
                      )}
                    </div>

                    <div>
                      <FormField label="Immediate treatment / First aid" htmlFor="urgent-treatment">
                        <Input
                          id="urgent-treatment"
                          type="text"
                          value={treatment}
                          onChange={(e) => setTreatment(e.target.value)}
                          placeholder="Medication, cold hose, anti-inflammatory, support bandage…"
                        />
                      </FormField>
                    </div>
                  </div>

                  <div>
                    <FormField label="Additional clinical notes" htmlFor="urgent-notes">
                      <Textarea
                        id="urgent-notes"
                        rows={2}
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                        placeholder="Observation notes, stable restrictions, or special care instructions…"
                      />
                    </FormField>
                  </div>
                </div>

                {/* 3. Training Clearance Decision */}
                <div className="space-y-4 border-t border-[var(--color-border)] pt-5">
                  <div className="flex items-center gap-1.5 text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                    <Icon name="shield" size={14} className="text-[var(--color-primary)]" />
                    <span>3. Veterinary Training Clearance Decision</span>
                    <span className="text-[var(--color-danger)]">*</span>
                  </div>

                  <div className="grid gap-3 sm:grid-cols-2">
                    {[
                      {
                        value: 'ALLOWED' as const,
                        label: 'ALLOWED',
                        desc: 'Fit for training. Horse may resume normal training schedule.',
                      },
                      {
                        value: 'BLOCKED' as const,
                        label: 'BLOCKED',
                        desc: 'Training suspended. Workouts cancelled until follow-up clearance.',
                      },
                    ].map((option) => (
                      <label
                        key={option.value}
                        className={`flex cursor-pointer flex-col rounded-[var(--radius-md)] border p-3.5 transition-all ${
                          trainingDecision === option.value
                            ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]'
                            : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-border-strong)]'
                        }`}
                      >
                        <div className="flex items-center gap-2.5">
                          <ChoiceInput
                            type="radio"
                            name="trainingDecision"
                            value={option.value}
                            checked={trainingDecision === option.value}
                            onChange={() => {
                              setTrainingDecision(option.value);
                              if (option.value === 'ALLOWED') {
                                setFieldErrors((prev) => ({
                                  ...prev,
                                  restrictionDetails: '',
                                  followUpDate: '',
                                  followUpDescription: '',
                                }));
                              }
                            }}
                          />
                          <span className="text-xs font-bold tracking-wide">{option.label}</span>
                        </div>
                        <p className="mt-1.5 text-xs text-[var(--color-text-secondary)] pl-6">
                          {option.desc}
                        </p>
                      </label>
                    ))}
                  </div>

                  {trainingDecision === 'BLOCKED' && (
                    <div className="mt-4 space-y-4 rounded-[var(--radius-md)] border border-[var(--color-warning)]/40 bg-[var(--color-warning-soft)]/20 p-4">
                      <div>
                        <FormField label="Training restriction & rest details" htmlFor="restriction-details" required>
                          <Textarea
                            id="restriction-details"
                            rows={2}
                            value={restrictionDetails}
                            onChange={(e) => {
                              setRestrictionDetails(e.target.value);
                              if (fieldErrors.restrictionDetails) {
                                setFieldErrors((prev) => ({ ...prev, restrictionDetails: '' }));
                              }
                            }}
                            placeholder="e.g. Strict box rest for 3 days, followed by 15-minute hand walking twice daily…"
                          />
                        </FormField>
                        {fieldErrors.restrictionDetails && (
                          <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.restrictionDetails}</p>
                        )}
                      </div>

                      <div className="grid gap-4 sm:grid-cols-2">
                        <div>
                          <FormField label="Follow-up examination date" htmlFor="follow-up-date" required>
                            <Input
                              id="follow-up-date"
                              type="date"
                              min={businessDate(1)}
                              value={followUpDate}
                              onChange={(e) => {
                                setFollowUpDate(e.target.value);
                                if (fieldErrors.followUpDate) setFieldErrors((prev) => ({ ...prev, followUpDate: '' }));
                              }}
                            />
                          </FormField>
                          <div className="mt-1.5 flex gap-1.5">
                            {[3, 7, 14].map((days) => (
                              <Button
                                type="button"
                                key={days}
                                size="sm"
                                variant="secondary"
                                onClick={() => setFollowUpDate(businessDate(days))}
                                className="transition-all active:scale-[0.97]"
                              >
                                +{days} days
                              </Button>
                            ))}
                          </div>
                          {fieldErrors.followUpDate && (
                            <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.followUpDate}</p>
                          )}
                        </div>

                        <div>
                          <FormField label="Follow-up clinical purpose" htmlFor="follow-up-desc" required>
                            <Input
                              id="follow-up-desc"
                              type="text"
                              value={followUpDescription}
                              onChange={(e) => {
                                setFollowUpDescription(e.target.value);
                                if (fieldErrors.followUpDescription) {
                                  setFieldErrors((prev) => ({ ...prev, followUpDescription: '' }));
                                }
                              }}
                            />
                          </FormField>
                          {fieldErrors.followUpDescription && (
                            <p className="mt-1 text-xs text-[var(--color-danger)]">{fieldErrors.followUpDescription}</p>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>

                {/* Submit Action */}
                <div className="border-t border-[var(--color-border)] pt-5">
                  <Button
                    type="submit"
                    variant="primary"
                    loading={submitting}
                    icon="check"
                    className="w-full sm:w-auto transition-all active:scale-[0.97]"
                  >
                    Complete Urgent Examination
                  </Button>
                </div>
              </form>
              )}
            </Panel>
          )}

          {/* Workflow: Examination Completed Summary */}
          {urgentCase.status === 'COMPLETED' && (
            <Panel padded className="space-y-4 border-l-4 border-l-[var(--color-success)] bg-[var(--color-surface)]">
              <div className="flex items-center gap-2.5 text-[var(--color-success)]">
                <Icon name="check" size={20} />
                <h3 className="text-base font-bold">Urgent Examination Completed</h3>
              </div>
              <p className="text-xs text-[var(--color-text-secondary)]">
                The veterinary clinical record and training decision have been recorded in the medical system.
              </p>

              {completedData && (
                <dl className="grid gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-4 text-xs sm:grid-cols-2">
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Diagnosis</dt>
                    <dd className="mt-1 font-bold text-sm text-[var(--color-text-primary)]">
                      {completedData.diagnosis}
                    </dd>
                  </div>
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Training Clearance</dt>
                    <dd className="mt-1">
                      <Pill tone={completedData.trainingDecision === 'BLOCKED' ? 'danger' : 'success'}>
                        {completedData.trainingDecision}
                      </Pill>
                    </dd>
                  </div>
                  <div className="sm:col-span-2">
                    <dt className="font-semibold text-[var(--color-text-muted)]">Examination Findings</dt>
                    <dd className="mt-1 text-[var(--color-text-secondary)] leading-relaxed">
                      {completedData.findings}
                    </dd>
                  </div>
                  {completedData.restrictionDetails && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Restriction Instructions</dt>
                      <dd className="mt-1 text-[var(--color-danger)] font-medium">
                        {completedData.restrictionDetails}
                      </dd>
                    </div>
                  )}
                  {completedData.treatment && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Treatment</dt>
                      <dd className="mt-1 text-[var(--color-text-secondary)] leading-relaxed">
                        {completedData.treatment}
                      </dd>
                    </div>
                  )}
                  {completedData.nextSchedule && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Scheduled Follow-up</dt>
                      <dd className="mt-1 text-[var(--color-text-primary)] font-semibold">
                        {completedData.nextSchedule.scheduledDate} — {completedData.nextSchedule.description}
                      </dd>
                    </div>
                  )}
                </dl>
              )}
            </Panel>
          )}
        </div>

        {/* Side Column (4 / 12) */}
        <div className="lg:col-span-4 space-y-5">
          {/* Horse Location & Medical Context */}
          <Panel padded className="space-y-3">
            <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2.5">
              <Icon name="building" size={15} className="text-[var(--color-primary)]" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-primary)]">
                Location & Patient Context
              </h3>
            </div>

            <dl className="space-y-2.5 text-xs">
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Horse</dt>
                <dd className="font-bold text-[var(--color-text-primary)]">
                  {urgentCase.horseName} (#{urgentCase.horseId})
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Stall Location</dt>
                <dd className="font-semibold text-[var(--color-text-primary)]">
                  {[urgentCase.stallCode, urgentCase.stableLocation].filter(Boolean).join(' · ') || 'Not assigned'}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Current Decision</dt>
                <dd className="font-bold text-[var(--color-danger)]">
                  {urgentCase.trainingDecision}
                </dd>
              </div>
            </dl>

            {urgentCase.previousTrainingDecision === 'BLOCKED' && (
              <div className="mt-2 rounded-[var(--radius-sm)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)]/30 p-2.5 text-xs text-[var(--color-warning)]">
                <strong>Pre-existing training hold:</strong> Horse was already blocked prior to this incident
                {urgentCase.previousTrainingDecisionReason ? `: ${urgentCase.previousTrainingDecisionReason}.` : '.'}
              </div>
            )}
          </Panel>

          {/* Reporter & Assignment Meta */}
          <Panel padded className="space-y-3">
            <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2.5">
              <Icon name="user" size={15} className="text-[var(--color-primary)]" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-primary)]">
                Incident Reporting Meta
              </h3>
            </div>

            <dl className="space-y-2.5 text-xs">
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Reported by</dt>
                <dd className="flex items-center gap-1.5 font-semibold text-[var(--color-text-primary)]">
                  <span>{urgentCase.reportedByName || 'Staff'}</span>
                  {urgentCase.reportedByRole && (
                    <span className="rounded bg-[var(--color-surface-muted)] px-1 py-0.5 text-[9px] font-bold uppercase text-[var(--color-text-secondary)] border border-[var(--color-border)]">
                      {urgentCase.reportedByRole}
                    </span>
                  )}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Reported at</dt>
                <dd className="font-medium text-[var(--color-text-secondary)]">
                  {formatDateTime(urgentCase.reportedAt)}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Assigned at</dt>
                <dd className="font-medium text-[var(--color-text-secondary)]">
                  {formatDateTime(urgentCase.assignedAt)}
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Case Severity</dt>
                <dd>
                  <Pill tone={urgentCase.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                    {urgentCase.severity}
                  </Pill>
                </dd>
              </div>
            </dl>
          </Panel>

          {/* Clinical Protocol Checklist */}
          <Panel padded className="space-y-3 bg-[var(--color-surface-subtle)]">
            <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2.5">
              <Icon name="clipboard" size={15} className="text-[var(--color-text-muted)]" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                Emergency Protocol Checklist
              </h3>
            </div>

            <ul className="space-y-2 text-xs text-[var(--color-text-secondary)]">
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="text-[var(--color-success)] mt-0.5 shrink-0" />
                <span>Verify horse is securely haltered and handler is positioned safely.</span>
              </li>
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="text-[var(--color-success)] mt-0.5 shrink-0" />
                <span>Record baseline vitals before administering any sedation or NSAIDs.</span>
              </li>
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="text-[var(--color-success)] mt-0.5 shrink-0" />
                <span>Palpate distal extremities for heat, digital pulse, or focal swelling.</span>
              </li>
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="text-[var(--color-success)] mt-0.5 shrink-0" />
                <span>If blocking training, establish a mandatory follow-up reassessment.</span>
              </li>
            </ul>
          </Panel>
        </div>
      </div>
    </div>
  );
}
