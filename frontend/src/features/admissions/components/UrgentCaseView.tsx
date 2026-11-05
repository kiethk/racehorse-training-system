'use client';

import React, { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Notice } from '@/components/ui/Notice';
import { Button, LinkButton } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { ProtectedImage } from '@/components/ui/ProtectedImage';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { EntityLink } from '@/components/ui/EntityLink';
import { entityHref } from '@/lib/entityRoute';
import { HorseVitalsPanel } from '@/features/health/components/HorseVitalsPanel';
import { admissionsApi } from '../services/api';
import { healthApi } from '@/features/health/services/healthApi';
import { displayError, formatDateTime } from '@/lib/display';
import { ClinicalExamForm, type ClinicalExamFormHandle } from './vet-review/ClinicalExamForm';
import { ApiError } from '@/services/api';
import type { HorseHealthMetric } from '@/features/horses/types';
import type {
  CompleteCareScheduleRequest,
  UrgentAssignmentAlert,
  UrgentCaseReport,
} from '../types';

export function UrgentCaseView({ scheduleId }: { scheduleId: number }) {
  const [urgentCase, setUrgentCase] = useState<UrgentAssignmentAlert | null>(null);
  const autoStartRef = useRef(false);
  const formRef = useRef<ClinicalExamFormHandle>(null);
  const [reports, setReports] = useState<UrgentCaseReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');
  const [completedData, setCompletedData] = useState<CompleteCareScheduleRequest | null>(null);
  const [completedMetrics, setCompletedMetrics] = useState<HorseHealthMetric[]>([]);

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- reset on id change
    setUrgentCase(null);
    setLoading(true);
    setReports([]);
    setError('');
    setCompletedData(null);
    setStarting(false);
    autoStartRef.current = false;
    setCompletedMetrics([]);

    admissionsApi
      .getUrgentCase(scheduleId)
      .then((data) => {
        if (!active) return;
        setUrgentCase(data);

        if (data.status === 'COMPLETED') {
          Promise.all([healthApi.getHealthRecords(data.horseId), healthApi.getHealthMetrics(data.horseId)])
            .then(([records, allMetrics]) => {
              if (!active) return;
              const rec = records.find((r) => r.careScheduleId === scheduleId) ?? null;
              if (!rec) return;
              setCompletedData({
                findings: rec.findings || '',
                diagnosis: rec.diagnosis || '',
                treatment: rec.treatment || undefined,
                notes: rec.notes || undefined,
                trainingDecision: rec.trainingDecision ?? 'BLOCKED',
                restrictionDetails: rec.restrictionDetails || undefined,
              });
              setCompletedMetrics((allMetrics || []).filter((m) => m.healthRecordId === rec.id));
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

  useEffect(() => {
    if (urgentCase?.status === 'SCHEDULED' && !autoStartRef.current) {
      autoStartRef.current = true;
      void startExam();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- one-shot, guarded by the ref above
  }, [urgentCase]);

  const handleSuccess = (
    _result: unknown,
    _kind: unknown,
    request: CompleteCareScheduleRequest | { findings?: unknown },
  ) => {
    if (!urgentCase) return;
    setCompletedData(request as CompleteCareScheduleRequest);
    setUrgentCase((current) => (current ? { ...current, status: 'COMPLETED' } : current));
    void healthApi
      .getHealthRecords(urgentCase.horseId)
      .then((records) => {
        const rec = records.find((r) => r.careScheduleId === scheduleId);
        if (!rec) return undefined;
        return healthApi.getHealthMetrics(urgentCase.horseId).then((m) => {
          setCompletedMetrics((m || []).filter((x) => x.healthRecordId === rec.id));
        });
      })
      .catch(() => {});
  };

  if (loading) {
    return (
      <Panel className="p-8 text-center text-sm text-[var(--color-text-secondary)]">
        <Icon name="refresh" size={20} className="mr-2 inline-block animate-spin" />
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
      <div>
        <Link
          href="/veterinarian/urgent"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-[var(--color-primary)] hover:underline"
        >
          <Icon name="arrow-left" size={13} />
          Back to Urgent Cases
        </Link>
      </div>

      <Panel className="overflow-hidden border border-[var(--color-border)] border-l-4 border-l-[var(--color-medical)] bg-[var(--color-surface)] p-5 sm:p-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-start gap-4">
            <HorseAvatar
              name={urgentCase.horseName}
              image={urgentCase.horseId ? `/api/horses/${urgentCase.horseId}/photo` : null}
              size={56}
              rounded="md"
            />
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <EntityLink href={entityHref('horse', urgentCase.horseId, 'VETERINARIAN')} className="text-xl font-bold text-[var(--color-text-primary)]">
                  {urgentCase.horseName}
                </EntityLink>
                <span className="rounded-[var(--radius-xs)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-1.5 py-0.5 font-mono text-xs text-[var(--color-text-muted)]">
                  #{urgentCase.horseId}
                </span>
                <Pill tone={urgentCase.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                  {urgentCase.severity}
                </Pill>
                <Pill
                  tone={
                    urgentCase.status === 'COMPLETED'
                      ? 'success'
                      : urgentCase.status === 'IN_PROGRESS'
                        ? 'info'
                        : 'warning'
                  }
                >
                  {urgentCase.status === 'SCHEDULED'
                    ? 'Pending Intake'
                    : urgentCase.status === 'IN_PROGRESS'
                      ? 'In Progress'
                      : 'Completed'}
                </Pill>
              </div>

              <h1 className="mt-1 text-base font-semibold text-[var(--color-text-primary)]">
                {urgentCase.title}
              </h1>

              <div className="mt-1.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--color-text-muted)]">
                {urgentCase.stallCode && (
                  <span className="flex items-center gap-1 font-medium text-[var(--color-text-secondary)]">
                    <Icon name="building" size={12} />
                    <span>Stall {urgentCase.stallCode}</span>
                    {urgentCase.stableLocation && (
                      <span className="text-[var(--color-text-muted)]">
                        ({urgentCase.stableLocation})
                      </span>
                    )}
                  </span>
                )}
                <span className="flex items-center gap-1">
                  <Icon name="clock" size={12} />
                  <span>Reported: {formatDateTime(urgentCase.reportedAt)}</span>
                </span>
                <span className="font-mono text-[11px]">Case #{urgentCase.scheduleId}</span>
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

      <div className="grid grid-cols-1 items-start gap-6 lg:grid-cols-12">
        <div className="space-y-6 lg:col-span-8">
          <Panel padded className="space-y-4">
            <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-3">
              <div className="flex items-center gap-2">
                <Icon name="file-text" size={16} className="text-[var(--color-medical)]" />
                <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                  Reported Incident &amp; Observation
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
                <label className="mb-2 block text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                  Photo Evidence
                </label>
                <div className="overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2">
                  <ProtectedImage
                    src={urgentCase.imageUrl}
                    alt="Urgent case report incident photo"
                    openOnClick
                    className="max-h-72 w-full rounded-[var(--radius-sm)] object-contain"
                  />
                  <p className="mt-1.5 text-center text-[11px] text-[var(--color-text-muted)]">
                    Click photo to open full resolution view
                  </p>
                </div>
              </div>
            )}
          </Panel>

          {additionalReports.length > 0 && (
            <Panel padded className="space-y-3">
              <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                Additional reports merged into this case ({additionalReports.length})
              </h2>
              <ul className="space-y-3">
                {additionalReports.map((report) => (
                  <li
                    key={report.id}
                    className="space-y-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3.5"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <strong className="text-sm text-[var(--color-text-primary)]">
                        {report.title}
                      </strong>
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
                        className="max-h-48 w-full rounded-[var(--radius-sm)] object-contain"
                      />
                    )}
                  </li>
                ))}
              </ul>
            </Panel>
          )}

          {urgentCase.status === 'SCHEDULED' && (
            <Panel padded className="border-l-4 border-l-[var(--color-warning)] bg-[var(--color-warning-soft)]/20">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                <div className="space-y-1">
                  <h3 className="text-sm font-bold text-[var(--color-text-primary)]">
                    Awaiting Clinical Intake
                  </h3>
                  <p className="text-xs text-[var(--color-text-secondary)]">
                    This urgent case is currently scheduled and assigned to you. Click Start
                    Examination to begin recording clinical findings.
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

          {urgentCase.status === 'IN_PROGRESS' && (
            <Panel padded className="space-y-6">
              <ClinicalExamForm
                ref={formRef}
                variant="URGENT"
                admissionId={0}
                horseId={urgentCase.horseId}
                careScheduleId={urgentCase.scheduleId}
                careType="URGENT"
                scheduleStatus={urgentCase.status}
                onSuccess={handleSuccess}
                prefillFindings={urgentCase.description}
              />
            </Panel>
          )}

          {urgentCase.status === 'COMPLETED' && (
            <Panel padded className="space-y-4 border-l-4 border-l-[var(--color-success)] bg-[var(--color-surface)]">
              <div className="flex items-center gap-2.5 text-[var(--color-success)]">
                <Icon name="check" size={20} />
                <h3 className="text-base font-bold">Urgent Examination Completed</h3>
              </div>
              <p className="text-xs text-[var(--color-text-secondary)]">
                The veterinary clinical record and training decision have been recorded in the
                medical system.
              </p>

              {completedData && (
                <dl className="grid gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-4 text-xs sm:grid-cols-2">
                  <div>
                    <dt className="font-semibold text-[var(--color-text-muted)]">Diagnosis</dt>
                    <dd className="mt-1 text-sm font-bold text-[var(--color-text-primary)]">
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
                    <dd className="mt-1 leading-relaxed text-[var(--color-text-secondary)]">
                      {completedData.findings}
                    </dd>
                  </div>
                  {completedData.restrictionDetails && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">
                        Restriction Instructions
                      </dt>
                      <dd className="mt-1 font-medium text-[var(--color-danger)]">
                        {completedData.restrictionDetails}
                      </dd>
                    </div>
                  )}
                  {completedData.treatment && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Treatment</dt>
                      <dd className="mt-1 leading-relaxed text-[var(--color-text-secondary)]">
                        {completedData.treatment}
                      </dd>
                    </div>
                  )}
                  {completedData.notes && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">Vet Assessment Notes</dt>
                      <dd className="mt-1 whitespace-pre-wrap leading-relaxed text-[var(--color-text-primary)]">
                        {completedData.notes}
                      </dd>
                    </div>
                  )}
                  {(completedData.nextSchedules?.length || 0) > 0 && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">
                        Scheduled Follow-ups
                      </dt>
                      {completedData.nextSchedules?.map((row) => (
                        <dd
                          key={row.idempotencyKey}
                          className="mt-1 font-semibold text-[var(--color-text-primary)]"
                        >
                          {row.scheduledDate} — {row.description}
                        </dd>
                      ))}
                    </div>
                  )}
                  {completedData.nextSchedule && (
                    <div className="sm:col-span-2">
                      <dt className="font-semibold text-[var(--color-text-muted)]">
                        Scheduled Follow-up
                      </dt>
                      <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">
                        {completedData.nextSchedule.scheduledDate} — {completedData.nextSchedule.description}
                      </dd>
                    </div>
                  )}
                </dl>
              )}

              {completedMetrics.length > 0 && <HorseVitalsPanel metrics={completedMetrics} />}

              <div className="flex justify-end">
                <LinkButton
                  href={`/veterinarian/records?scheduleId=${scheduleId}&back=urgent`}
                  variant="secondary"
                  size="sm"
                  icon="file-text"
                >
                  View full record
                </LinkButton>
              </div>
            </Panel>
          )}
        </div>

        <div className="space-y-5 lg:col-span-4">
          <Panel padded className="space-y-3">
            <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2.5">
              <Icon name="building" size={15} className="text-[var(--color-primary)]" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-primary)]">
                Location &amp; Patient Context
              </h3>
            </div>

            <dl className="space-y-2.5 text-xs">
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Horse</dt>
                <dd className="font-bold text-[var(--color-text-primary)]">
                  <EntityLink href={entityHref('horse', urgentCase.horseId, 'VETERINARIAN')}>{urgentCase.horseName} (#{urgentCase.horseId})</EntityLink>
                </dd>
              </div>
              <div className="flex items-center justify-between">
                <dt className="text-[var(--color-text-muted)]">Stall Location</dt>
                <dd className="font-semibold text-[var(--color-text-primary)]">
                  {[urgentCase.stallCode, urgentCase.stableLocation].filter(Boolean).join(' · ') ||
                    'Not assigned'}
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
                <strong>Pre-existing training hold:</strong> Horse was already blocked prior to this
                incident
                {urgentCase.previousTrainingDecisionReason
                  ? `: ${urgentCase.previousTrainingDecisionReason}.`
                  : '.'}
              </div>
            )}
          </Panel>

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
                    <span className="rounded border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-1 py-0.5 text-[9px] font-bold uppercase text-[var(--color-text-secondary)]">
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

          <Panel padded className="space-y-3 bg-[var(--color-surface-subtle)]">
            <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2.5">
              <Icon name="clipboard" size={15} className="text-[var(--color-text-muted)]" />
              <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">
                Emergency Protocol Checklist
              </h3>
            </div>

            <ul className="space-y-2 text-xs text-[var(--color-text-secondary)]">
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="mt-0.5 shrink-0 text-[var(--color-success)]" />
                <span>Verify horse is securely haltered and handler is positioned safely.</span>
              </li>
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="mt-0.5 shrink-0 text-[var(--color-success)]" />
                <span>Record baseline vitals before administering any sedation or NSAIDs.</span>
              </li>
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="mt-0.5 shrink-0 text-[var(--color-success)]" />
                <span>Palpate distal extremities for heat, digital pulse, or focal swelling.</span>
              </li>
              <li className="flex items-start gap-2">
                <Icon name="check" size={13} className="mt-0.5 shrink-0 text-[var(--color-success)]" />
                <span>If blocking training, establish a mandatory follow-up reassessment.</span>
              </li>
            </ul>
          </Panel>
        </div>
      </div>
    </div>
  );
}
