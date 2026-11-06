/* eslint-disable no-restricted-syntax */
'use client';

import React, { useCallback, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { Notice } from '@/components/ui/Notice';
import { Button, LinkButton } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { ProtectedImage } from '@/components/ui/ProtectedImage';
import { SchedulePatientHeader } from '@/features/care/components/SchedulePatientHeader';
import { HorseVitalsPanel } from '@/features/health/components/HorseVitalsPanel';
import { admissionsApi } from '../services/api';
import { healthApi } from '@/features/health/services/healthApi';
import { displayError, formatDateTime } from '@/lib/display';
import { ClinicalExamForm, type ClinicalExamFormHandle } from './vet-review/ClinicalExamForm';
import { INCIDENT_SEVERITY_MAP } from '@/features/groom/lib/incidentDisplay';
import { ApiError } from '@/services/api';
import type { HorseHealthMetric } from '@/features/horses/types';
import type {
  CareScheduleDetail,
  CompleteCareScheduleRequest,
  UrgentAssignmentAlert,
  UrgentCaseReport,
} from '../types';

export function UrgentCaseView({ scheduleId }: { scheduleId: number }) {
  const [urgentCase, setUrgentCase] = useState<UrgentAssignmentAlert | null>(null);
  const autoStartRef = useRef(false);
  const formRef = useRef<ClinicalExamFormHandle>(null);
  const [reports, setReports] = useState<UrgentCaseReport[]>([]);
  const [showAdditional, setShowAdditional] = useState(false);
  const [loading, setLoading] = useState(true);
  const [starting, setStarting] = useState(false);
  const [error, setError] = useState('');
  const [completedData, setCompletedData] = useState<CompleteCareScheduleRequest | null>(null);
  const [completedMetrics, setCompletedMetrics] = useState<HorseHealthMetric[]>([]);
  const [waitingFollowUp, setWaitingFollowUp] = useState<CareScheduleDetail['waitingFollowUp']>(null);

  const loadWaitingFollowUp = useCallback(() => {
    admissionsApi
      .getCareScheduleDetail(scheduleId)
      .then((detail) => setWaitingFollowUp(detail.waitingFollowUp ?? null))
      .catch(() => undefined);
  }, [scheduleId]);

  useEffect(() => {
    loadWaitingFollowUp();
  }, [loadWaitingFollowUp]);

  useEffect(() => {
    let active = true;
    // eslint-disable-next-line react-hooks/set-state-in-effect
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
    loadWaitingFollowUp();
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

  const urgentAsideTop = (
    <div className="space-y-4">
      <Panel padded className="space-y-2.5">
        <div className="flex items-center gap-2 border-b border-[var(--color-border)] pb-2">
          <Icon name="building" size={14} className="text-[var(--color-primary)]" />
          <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-primary)]">
            Patient &amp; Location
          </h3>
        </div>
        <dl className="space-y-2 text-xs">
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
            <dt className="text-[var(--color-text-muted)]">Severity</dt>
            <dd>
              <Pill tone={urgentCase.severity === 'CRITICAL' ? 'danger' : 'warning'}>
                {urgentCase.severity}
              </Pill>
            </dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-[var(--color-text-muted)]">Reported by</dt>
            <dd className="font-medium text-[var(--color-text-secondary)]">
              {urgentCase.reportedByName || 'Staff'} ({formatDateTime(urgentCase.reportedAt)})
            </dd>
          </div>
        </dl>
      </Panel>
    </div>
  );

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

      {/* Case Header */}
      <SchedulePatientHeader
        scheduleId={urgentCase.scheduleId}
        status={urgentCase.status}
        careType="URGENT"
        scheduledAt={urgentCase.reportedAt}
        horseName={urgentCase.horseName}
        horseId={urgentCase.horseId}
        severity={urgentCase.severity}
        action={
          (urgentCase.status === 'SCHEDULED' || urgentCase.status === 'PAUSED' || urgentCase.status === 'OVERDUE') && (
            <Button
              variant="destructive"
              icon="activity"
              loading={starting}
              onClick={startExam}
              className="transition-all active:scale-[0.97]"
            >
              Start Examination
            </Button>
          )
        }
      />

      {/* Reported Incident & Observations from Groom */}
      <Panel padded className="space-y-3">
        <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2.5">
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

        <div className="flex flex-col sm:flex-row gap-4 items-start">
          {urgentCase.imageUrl && (
            <div className="group relative h-28 w-28 sm:h-32 sm:w-32 shrink-0 overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] shadow-xs transition-all hover:border-[var(--color-primary)] hover:shadow-sm">
              <ProtectedImage
                src={urgentCase.imageUrl}
                alt="Urgent case report incident photo"
                openOnClick
                className="h-28 w-28 sm:h-32 sm:w-32 object-cover transition-transform duration-200 group-hover:scale-105"
              />
              <div className="pointer-events-none absolute bottom-1.5 right-1.5 flex items-center gap-1 rounded bg-black/60 px-1.5 py-0.5 text-[10px] font-medium text-white shadow-xs backdrop-blur-xs">
                <Icon name="search" size={10} />
                <span>Zoom</span>
              </div>
            </div>
          )}

          <div className="min-w-0 flex-1 space-y-2">
            <p className="whitespace-pre-wrap break-words text-sm leading-relaxed text-[var(--color-text-secondary)]">
              {urgentCase.description || 'No detailed clinical description provided by reporter.'}
            </p>
            <div className="flex items-center gap-2 text-xs text-[var(--color-text-muted)]">
              <span>Reported at {formatDateTime(urgentCase.reportedAt)}</span>
            </div>
          </div>
        </div>

        {additionalReports.length > 0 && (
          <div className="border-t border-[var(--color-border)] pt-2.5">
            <button
              type="button"
              onClick={() => setShowAdditional((prev) => !prev)}
              className="flex w-full items-center justify-between rounded-[var(--radius-md)] bg-[var(--color-surface-subtle)] px-3 py-2 text-xs font-semibold text-[var(--color-text-primary)] transition-all hover:bg-[var(--color-surface)] hover:text-[var(--color-primary)] active:scale-[0.99]"
            >
              <div className="flex items-center gap-2">
                <Icon
                  name={showAdditional ? 'chevron-up' : 'chevron-down'}
                  size={14}
                  className="text-[var(--color-primary)]"
                />
                <span>Additional merged reports ({additionalReports.length})</span>
              </div>
              <span className="text-[11px] font-normal text-[var(--color-text-muted)]">
                {showAdditional ? 'Hide' : 'View'}
              </span>
            </button>

            {showAdditional && (
              <div className="mt-3 space-y-3">
                {additionalReports.map((report) => {
                  const severity = INCIDENT_SEVERITY_MAP[report.severity];
                  return (
                    <div
                      key={report.id}
                      className="flex flex-col sm:flex-row items-start gap-3.5 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-3"
                    >
                      {report.imageUrl && (
                        <div className="group relative h-20 w-20 sm:h-22 sm:w-22 shrink-0 overflow-hidden rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-xs transition-all hover:border-[var(--color-primary)] hover:shadow-sm">
                          <ProtectedImage
                            src={report.imageUrl}
                            alt={`Report: ${report.title}`}
                            openOnClick
                            className="h-20 w-20 sm:h-22 sm:w-22 object-cover transition-transform duration-200 group-hover:scale-105"
                          />
                          <div className="pointer-events-none absolute bottom-1 right-1 flex items-center gap-0.5 rounded bg-black/60 px-1 py-0.5 text-[9px] font-medium text-white shadow-xs backdrop-blur-xs">
                            <Icon name="search" size={9} />
                            <span>Zoom</span>
                          </div>
                        </div>
                      )}

                      <div className="min-w-0 flex-1 space-y-1">
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <strong className="text-xs font-bold text-[var(--color-text-primary)]">
                            {report.title}
                          </strong>
                          <Pill tone={severity.tone} size="sm">
                            {severity.label}
                          </Pill>
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
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}
      </Panel>

      {/* Main Workspace Area */}
      {(urgentCase.status === 'SCHEDULED' || urgentCase.status === 'PAUSED') && (
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

      {(urgentCase.status === 'IN_PROGRESS' || urgentCase.status === 'DELAYED' || urgentCase.status === 'OVERDUE') && (
        <ClinicalExamForm
          ref={formRef}
          variant="STANDARD"
          admissionId={0}
          horseId={urgentCase.horseId}
          candidateName={urgentCase.horseName}
          careScheduleId={urgentCase.scheduleId}
          careType="URGENT"
          scheduleStatus={urgentCase.status}
          asideTop={urgentAsideTop}
          onSuccess={handleSuccess}
          prefillFindings={urgentCase.description}
          waitingFollowUpAt={waitingFollowUp?.at}
        />
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
              {completedData.nextSchedule && (
                <div className="sm:col-span-2">
                  <dt className="font-semibold text-[var(--color-text-muted)]">
                    Scheduled Follow-up
                  </dt>
                  <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">
                    {waitingFollowUp
                      ? `${formatDateTime(waitingFollowUp.at)}, ${waitingFollowUp.veterinarianName ?? 'awaiting a vet'}`
                      : completedData.nextSchedule.scheduledDate}{' '}
                    — {completedData.nextSchedule.description}
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
  );
}