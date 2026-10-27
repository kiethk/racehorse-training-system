'use client';
import { hydrationStatusLabel } from '@/features/health/lib/hydrationStatus';

import { useCallback, useEffect, useState } from 'react';
import { Button, FilterChips, Icon, Modal, Notice, Panel, Pill, Spinner } from '@/components/ui';
import { BackLink } from '@/components/ui/BackLink';
import { LinkButton } from '@/components/ui/Button';
import { EmptyState } from '@/components/ui/states';
import dynamic from 'next/dynamic';
import { careService } from '../services/careService';
import { healthApi } from '@/features/health/services/healthApi';
import type { HealthRecord } from '@/features/health/types';
import type { InjuryRecord } from '@/features/health/types';
import type { InjuryMarkerData } from '@/features/vet/components/UrgentHorseInjuryViewer';
import { zoneById, resolveInjuryWorldPosition, resolveInjuryNormal, isNormalizedPoint } from '@/features/vet/lib/injuryZones';
import { TreatmentPlanModal } from '../shared/components/TreatmentPlanModal';
import { PrescriptionModal } from './PrescriptionModal';
import type { CareScheduleDetailResponse, TreatmentPlan } from '../types';
import type { HorsePreventiveStatus } from '../types/preventive';
import type { HorseHealthMetric } from '@/features/horses/types';
import { displayError, formatDate, formatDateTime } from '@/lib/display';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import { HorseAvatar } from '@/components/ui/HorseAvatar';

// The 3D canvas (three.js + GLB) must stay out of the server bundle.
const UrgentHorseInjuryViewer = dynamic(
  () => import('@/features/vet/components/UrgentHorseInjuryViewer'),
  { ssr: false, loading: () => <div className="h-[420px] w-full rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] animate-pulse" /> },
);

export interface RecordDetailViewProps {
  /** Primary mode: load the completed care schedule + its clinical record. */
  scheduleId?: number | null;
  /** Alternative mode: load a specific record of a horse (Health registry entry). */
  recordId?: number | null;
  /** Horse context, required in recordId mode. */
  horseId?: number | null;
  /** Pre-fetched complete record — skips the record fetch. */
  record?: HealthRecord | null;
  horseName?: string;
  /** Where the logical back button returns to (defaults to the vet dashboard). */
  backHref?: string;
  /** Set by the modal shell: the overlay already provides closing affordances. */
  onHideBack?: boolean;
  onScheduleUpdated?: () => void;
}

/** Newest examination first. */
function sortByExamined(records: HealthRecord[]) {
  return [...records].sort((a, b) => {
    const ta = a.examinedAt ? new Date(a.examinedAt).getTime() : 0;
    const tb = b.examinedAt ? new Date(b.examinedAt).getTime() : 0;
    return tb - ta;
  });
}

/** Pill tone for an injury status that the backend actually stores. */
function injuryStatusTone(status: string | null) {
  if (status === 'ACTIVE') return 'danger';
  if (status === 'RECOVERED') return 'success';
  return 'neutral';
}

/** Pill tone for a PreventiveDueStatus value (OK/DUE_SOON/OVERDUE/MISSING/NONE). */
function preventiveTone(status: string) {
  if (status === 'OVERDUE' || status === 'MISSING') return 'danger';
  if (status === 'DUE_SOON') return 'warning';
  if (status === 'NONE') return 'neutral';
  return 'success';
}

export function RecordDetailView({
  scheduleId = null,
  recordId = null,
  horseId = null,
  record = null,
  horseName,
  backHref = '/veterinarian',
  onHideBack = false,
  onScheduleUpdated,
}: RecordDetailViewProps) {
  const [loading, setLoading] = useState(true);
  const [detail, setDetail] = useState<CareScheduleDetailResponse | null>(null);
  const [fullRecord, setFullRecord] = useState<HealthRecord | null>(null);
  const [horseRecords, setHorseRecords] = useState<HealthRecord[]>([]);
  const [activeRecordId, setActiveRecordId] = useState<number | null>(null);
  /** The horse's current training decision (may differ from the record's exam-time decision). */
  const [currentDecision, setCurrentDecision] = useState<string | null>(null);
  const [plans, setPlans] = useState<TreatmentPlan[]>([]);
  const [metrics, setMetrics] = useState<HorseHealthMetric[]>([]);
  const [injuries, setInjuries] = useState<InjuryRecord[]>([]);
  const [preventive, setPreventive] = useState<HorsePreventiveStatus | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [showInjuryMap, setShowInjuryMap] = useState(false);
  const [activePlanForPrescription, setActivePlanForPrescription] = useState<{
    id: number;
    name: string;
  } | null>(null);
  const [expandedMarkerId, setExpandedMarkerId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      let rec = record ?? null;
      let det: CareScheduleDetailResponse | null = null;

      if (rec == null && recordId != null) {
        if (horseId == null) {
          throw new Error('This record was opened without its horse context. Go back and open it again.');
        }
        const recs = await healthApi.getHealthRecords(horseId);
        rec = recs.find((r) => r.id === recordId) ?? null;
        if (!rec) throw new Error('Clinical record not found for this horse.');
        // Same layout serves every record of the horse through the selector.
        const sorted = sortByExamined(recs);
        setHorseRecords(sorted);
        setActiveRecordId(recordId);
      }

      // Horse mode (no specific record): open the most recent examination.
      if (rec == null && recordId == null && scheduleId == null && horseId != null) {
        const recs = await healthApi.getHealthRecords(horseId).catch(() => []);
        const sorted = sortByExamined(recs || []);
        setHorseRecords(sorted);
        rec = sorted[0] ?? null;
        setActiveRecordId(rec?.id ?? null);
      }

      if (det == null && rec == null && scheduleId != null) {
        det = await careService.getScheduleDetail(scheduleId);
      }

      if (det == null && rec?.careScheduleId != null) {
        try {
          det = await careService.getScheduleDetail(rec.careScheduleId);
        } catch {
          // The record alone is still fully viewable.
        }
      }

      // The schedule detail DTO carries only a subset of HealthRecord fields;
      // enrich with the complete entity so every field is shown.
      if (det != null && rec == null && det.schedule?.horseId != null) {
        try {
          const recs = await healthApi.getHealthRecords(det.schedule.horseId);
          rec = recs.find((r) => r.careScheduleId === det.schedule.id) ?? null;
        } catch {
          rec = null;
        }
      }

      setDetail(det);
      setFullRecord(rec);

      const resolvedHorseId = det?.schedule?.horseId ?? rec?.horseId ?? null;
      if (resolvedHorseId != null) {
        try {
          const m = await healthApi.getHealthMetrics(resolvedHorseId);
          setMetrics(m || []);
        } catch {
          setMetrics([]);
        }
        try {
          const inj = await healthApi.getHorseInjuries(resolvedHorseId);
          setInjuries(inj || []);
        } catch {
          setInjuries([]);
        }
        try {
          setPreventive(await healthApi.getPreventiveStatus(resolvedHorseId));
        } catch {
          setPreventive(null);
        }
        try {
          const sum = await healthApi.getHealthSummary(resolvedHorseId);
          setCurrentDecision(sum?.trainingDecision ?? null);
        } catch {
          setCurrentDecision(null);
        }
      }

      const plansFor = rec?.id ?? det?.healthRecord?.id ?? null;
      setPlans(plansFor != null ? await careService.getTreatmentPlans(plansFor).catch(() => []) : []);
    } catch (err) {
      setError(displayError(err, 'Failed to load the clinical record.'));
    } finally {
      setLoading(false);
    }
  }, [scheduleId, recordId, horseId, record]);

  useEffect(() => {
    Promise.resolve().then(() => void load());
  }, [load]);

  /** Switch to another record of the same horse inside the shared layout. */
  const selectRecord = useCallback(
    async (id: number) => {
      const rec = horseRecords.find((r) => r.id === id);
      if (!rec) return;
      setActiveRecordId(id);
      setFullRecord(rec);
      setDetail(null);
      if (rec.careScheduleId != null) {
        try {
          setDetail(await careService.getScheduleDetail(rec.careScheduleId));
        } catch {
          // The record alone is still fully viewable.
        }
      }
      setPlans(await careService.getTreatmentPlans(id).catch(() => []));
    },
    [horseRecords],
  );

  const backLabel = backHref.includes('health')
    ? 'Back to Health'
    : backHref.includes('routine')
      ? 'Back to Routine Care'
      : backHref.includes('schedule')
        ? 'Back to Schedule'
        : 'Back to dashboard';

  // Horse mode with no records at all: a clear empty state instead of a bare warning.
  if (!loading && !error && !detail?.schedule && !fullRecord) {
    if (horseId != null && recordId == null && scheduleId == null && record == null) {
      return (
        <div className="space-y-4">
          {!onHideBack && <BackLink href={backHref}>{backLabel}</BackLink>}
          <Panel padded className="py-10">
            <EmptyState
              icon="clipboard"
              title="No clinical records for this horse"
              description="This horse has no clinical examination on file yet. Schedule a routine examination to create the first record."
              action={
                <LinkButton href="/veterinarian/routine" icon="calendar">
                  Open Routine Care
                </LinkButton>
              }
            />
          </Panel>
        </div>
      );
    }
    return <Notice tone="warning">No clinical record to display.</Notice>;
  }

  const resolvedHorseId = detail?.schedule?.horseId ?? fullRecord?.horseId ?? null;
  const resolvedHorseName = detail?.horse?.name || horseName || (resolvedHorseId != null ? `#${resolvedHorseId}` : '—');
  const recordIdShown = activeRecordId ?? detail?.healthRecord?.id ?? fullRecord?.id ?? null;
  const trainingDecision = detail?.healthRecord?.trainingDecision ?? fullRecord?.trainingDecision ?? null;
  const restrictionDetails = detail?.healthRecord?.restrictionDetails ?? fullRecord?.restrictionDetails ?? null;
  // Injuries and vitals are fetched per horse; scope them to the record being
  // viewed so an old record never shows a newer examination's data.
  const recordInjuries = recordIdShown != null
    ? injuries.filter((i) => i.healthRecordId === recordIdShown)
    : injuries;
  const recordMetrics = recordIdShown != null
    ? metrics.filter((m) => m.healthRecordId === recordIdShown)
    : metrics;
  // Metric lists come newest-first, but sort explicitly so display never depends on API ordering.
  const latestMetric = recordMetrics.length > 0
    ? [...recordMetrics].sort((a, b) => b.recordedAt.localeCompare(a.recordedAt))[0]
    : null;
  // Only injuries actually captured on the 3D model have coordinates; the rest
  // stay in the tag list without a marker instead of being dropped at origin.
  const markerData: InjuryMarkerData[] = recordInjuries
    .filter((i) => i.positionX != null)
    .map((i) => {
      const worldPos = resolveInjuryWorldPosition(i);
      const normal =
        i.normalX != null && i.normalY != null && i.normalZ != null
          ? resolveInjuryNormal([Number(i.normalX), Number(i.normalY), Number(i.normalZ)], worldPos)
          : resolveInjuryNormal(undefined, worldPos);
      // Seeded markers carry no real surface hit; their coordinates need the
      // runtime snap instead of trusting the stale bbox mapping.
      const needsSurfaceSnap =
        i.normalX == null ||
        isNormalizedPoint({
          x: Number(i.positionX),
          y: Number(i.positionY),
          z: Number(i.positionZ),
        });
      return {
        id: String(i.id),
        position: worldPos,
        normal,
        needsSurfaceSnap,
        zoneId: i.zoneId ?? undefined,
        label: (i.zoneId && zoneById(i.zoneId)?.label) || i.injuryType,
        severity: i.severity ?? undefined,
        status: i.status ?? undefined,
        diagnosedAt: i.diagnosedAt ?? undefined,
        description: i.description ?? undefined,
      };
    });

  return (
    <div className="space-y-4">
      {!onHideBack && <BackLink href={backHref}>{backLabel}</BackLink>}

      {loading ? (
        <div className="flex justify-center py-16">
          <Spinner size="lg" />
        </div>
      ) : error ? (
        <Notice tone="error" title="Failed to load details">
          <div className="flex items-center justify-between gap-2">
            <span>{error}</span>
            <Button variant="secondary" size="sm" onClick={load}>
              Retry
            </Button>
          </div>
        </Notice>
      ) : (
        <div className="space-y-4">
          {/* 1. Patient information header (Admission-style) */}
          <Panel padded className="border-l-4 border-l-[var(--color-medical)]">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div className="flex items-start gap-3.5">
                <HorseAvatar name={resolvedHorseName} image={null} size={50} rounded="md" />
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <h1 className="text-xl font-bold tracking-tight text-[var(--color-text-primary)]">
                      {resolvedHorseName}
                    </h1>
                    {detail?.horse?.breed && (
                      <span className="text-xs text-[var(--color-text-muted)] font-medium">
                        ({detail.horse.breed})
                      </span>
                    )}
                    {(() => {
                      // Synced with the Health list status: the horse's current decision,
                      // falling back to the record's own decision when the summary is unavailable.
                      const decision = currentDecision ?? trainingDecision ?? null;
                      return decision ? (
                        <Pill tone={decision === 'BLOCKED' ? 'danger' : 'success'} size="md">
                          {decision}
                        </Pill>
                      ) : null;
                    })()}
                  </div>
                  <div className="mt-1 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-[var(--color-text-secondary)]">
                    {fullRecord?.examinedAt && (
                      <span className="inline-flex items-center gap-1">
                        <Icon name="calendar" size={12} />
                        Examined: {formatDateTime(fullRecord.examinedAt)}
                      </span>
                    )}
                    {fullRecord?.recordType && (
                      <span>
                        Type: <strong>{fullRecord.recordType}</strong>
                      </span>
                    )}
                    {detail?.schedule && (
                      <span className="font-mono text-[11px]">Schedule #{detail.schedule.id}</span>
                    )}
                    {detail?.veterinarian?.fullName && (
                      <span>
                        Veterinarian: <strong>{detail.veterinarian.fullName}</strong>
                      </span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </Panel>

          {/* Training restriction notice (only what the backend stored) */}
          {restrictionDetails && (
            <Notice tone="warning" title="Training Restriction Details">
              {restrictionDetails}
            </Notice>
          )}

          {/* Record selector — every record of the horse through the same layout */}
          {horseRecords.length > 1 && (
            <FilterChips
              label="Record"
              options={horseRecords.map((r) => ({
                value: String(r.id),
                label: `#${r.id} · ${r.examinedAt ? formatDate(r.examinedAt) : 'Undated'}`,
              }))}
              value={String(activeRecordId ?? '')}
              onChange={(v) => void selectRecord(Number(v))}
            />
          )}

          {/* 2. Clinical record content */}
          {recordIdShown ? (
            <Panel padded className="space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] pb-2">
                <h2 className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-1.5">
                  <Icon name="clipboard" size={16} className="text-[var(--color-primary)]" />
                  Clinical Record #{recordIdShown}
                </h2>
              </div>

              <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 text-sm">
                <div>
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Symptoms:
                  </span>
                  <p className="text-[var(--color-text-primary)]">
                    {fullRecord?.symptoms || 'None reported.'}
                  </p>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Diagnosis:
                  </span>
                  <p className="text-[var(--color-text-primary)]">
                    {detail?.healthRecord?.diagnosis || fullRecord?.diagnosis || 'None diagnosed.'}
                  </p>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Findings:
                  </span>
                  <p className="text-[var(--color-text-primary)]">
                    {detail?.healthRecord?.findings || fullRecord?.findings || 'No abnormalities detected.'}
                  </p>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Administered Treatment:
                  </span>
                  <p className="text-[var(--color-text-secondary)]">
                    {detail?.healthRecord?.treatment || fullRecord?.treatment || 'Not recorded.'}
                  </p>
                </div>
              </div>

              {fullRecord?.productOrService && (
                <div className="text-sm">
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Product / Service:
                  </span>
                  <p className="text-[var(--color-text-secondary)]">{fullRecord.productOrService}</p>
                </div>
              )}

              {fullRecord?.notes && (
                <div className="text-sm">
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Notes:
                  </span>
                  <p className="text-[var(--color-text-secondary)]">{fullRecord.notes}</p>
                </div>
              )}
            </Panel>
          ) : (
            <Panel padded className="text-sm text-[var(--color-text-muted)] italic">
              No clinical health record has been completed for this schedule yet.
            </Panel>
          )}

          {/* 3. Vitals (latest telemetry) */}
          <Panel padded className="space-y-3">
            <h2 className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-1.5">
              <Icon name="heart-pulse" size={16} className="text-[var(--color-medical)]" />
              Vitals
            </h2>
            {latestMetric ? (
              <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-4 text-xs">
                {latestMetric.heartRate != null && (
                  <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                    <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                      Heart Rate
                    </span>
                    <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                      {latestMetric.heartRate} bpm
                    </span>
                  </div>
                )}
                {latestMetric.temperature != null && (
                  <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                    <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                      Temperature
                    </span>
                    <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                      {latestMetric.temperature} °C
                    </span>
                  </div>
                )}
                {latestMetric.respiratoryRate != null && (
                  <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                    <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                      Respiration
                    </span>
                    <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                      {latestMetric.respiratoryRate} rpm
                    </span>
                  </div>
                )}
                {latestMetric.weight != null && (
                  <div className="rounded bg-[var(--color-surface-subtle)] p-2 border border-[var(--color-border)]">
                    <span className="text-[var(--color-text-muted)] block text-[10px] uppercase font-bold">
                      Weight
                    </span>
                    <span className="font-metric text-sm font-bold text-[var(--color-text-primary)]">
                      {latestMetric.weight} kg
                    </span>
                  </div>
                )}
                {latestMetric.hydrationStatus && (
                  <div className="col-span-2 sm:col-span-4 text-xs">
                    <span className="text-[var(--color-text-muted)]">Hydration: </span>
                    <span className="font-semibold">{hydrationStatusLabel(latestMetric.hydrationStatus)}</span>
                  </div>
                )}
              </div>
            ) : (
              <p className="text-xs text-[var(--color-text-muted)] py-2 text-center">No vitals recorded yet.</p>
            )}
          </Panel>

          {/* 4. Injury — list + button opening the 3D map modal */}
          <Panel padded className="space-y-3">
            <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-2">
              <h2 className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-1.5">
                <Icon name="alert-triangle" size={16} className="text-[var(--color-danger)]" />
                Injury
              </h2>
              {markerData.length > 0 && (
                <Button
                  variant="secondary"
                  size="sm"
                  icon="activity"
                  onClick={() => setShowInjuryMap(true)}
                  className="transition-all active:scale-[0.98]"
                >
                  View 3D Injury Map
                </Button>
              )}
            </div>

            {recordInjuries.length === 0 ? (
              <p className="text-xs text-[var(--color-text-muted)] py-2 text-center">
                No injuries recorded in this examination.
              </p>
            ) : (
              <div className="space-y-1.5">
                {recordInjuries.map((inj) => {
                  const isOpen = expandedMarkerId === String(inj.id);
                  const hasMarker = inj.positionX != null;
                  const zoneLabel = (inj.zoneId && zoneById(inj.zoneId)?.label) || 'Unmapped zone';
                  return (
                    <div
                      key={inj.id}
                      className="rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)]"
                    >
                      <Button
                        type="button"
                        variant="tertiary"
                        size="sm"
                        aria-expanded={isOpen}
                        onClick={() => setExpandedMarkerId(isOpen ? null : String(inj.id))}
                        className="flex w-full items-center justify-between gap-2 px-2.5 py-1.5 text-left text-xs font-medium hover:bg-[var(--color-surface-muted)]"
                      >
                        <span className="flex min-w-0 items-center gap-1.5">
                          <Icon name={isOpen ? 'chevron-down' : 'chevron-right'} size={12} className="text-[var(--color-text-muted)] shrink-0" />
                          <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-[var(--color-danger)]" aria-hidden />
                          <span className="truncate font-semibold">
                            {inj.injuryType}
                          </span>
                          <span className="text-[var(--color-text-secondary)]">
                            · {zoneLabel}
                          </span>
                        </span>
                        <span className="flex shrink-0 items-center gap-2">
                          {!hasMarker && (
                            <span className="text-[10px] text-[var(--color-text-muted)]">no 3D marker</span>
                          )}
                          <Pill tone={injuryStatusTone(inj.status)} size="sm">
                            {inj.status}
                          </Pill>
                        </span>
                      </Button>

                      {isOpen && (
                        <div className="space-y-1.5 border-t border-[var(--color-border)] px-3 py-2 text-xs text-[var(--color-text-secondary)]">
                          <div className="flex flex-wrap gap-x-4 gap-y-1">
                            <span>
                              <span className="font-semibold text-[var(--color-text-primary)]">Severity:</span>{' '}
                              {inj.severity || 'Not recorded'}
                            </span>
                            <span>
                              <span className="font-semibold text-[var(--color-text-primary)]">Diagnosed:</span>{' '}
                              {formatDate(inj.diagnosedAt)}
                            </span>
                            {inj.recoveredAt && (
                              <span>
                                <span className="font-semibold text-[var(--color-text-primary)]">Recovered:</span>{' '}
                                {formatDate(inj.recoveredAt)}
                              </span>
                            )}
                          </div>
                          {inj.expectedRecoveryDate && (
                            <div>
                              <span className="font-semibold text-[var(--color-text-primary)]">Expected recovery:</span>{' '}
                              {formatDate(inj.expectedRecoveryDate)}
                            </div>
                          )}
                          {inj.description && (
                            <p className="leading-relaxed">{inj.description}</p>
                          )}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </Panel>

          {/* 5. Preventive care (backend protocol statuses) */}
          <Panel padded className="space-y-3">
            <h2 className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-1.5">
              <Icon name="shield" size={16} className="text-[var(--color-medical)]" />
              Preventive Care
            </h2>
            {preventive && preventive.protocols.length > 0 ? (
              <div className="space-y-2">
                {preventive.protocols.map((p) => (
                  <div
                    key={p.protocolId}
                    className="flex items-center justify-between rounded-md border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2.5"
                  >
                    <div className="min-w-0 flex-1 pr-2">
                      <div className="font-semibold text-xs text-[var(--color-text-primary)] truncate">
                        {p.name}{' '}
                        <span className="text-[var(--color-text-muted)] font-mono text-[10px]">
                          ({p.code})
                        </span>
                      </div>
                      <div className="text-[11px] text-[var(--color-text-secondary)] mt-0.5">
                        Due: <strong>{p.nextDueOn ? formatDate(p.nextDueOn) : 'Immediate'}</strong>
                      </div>
                    </div>
                    <Pill tone={preventiveTone(p.status)} size="sm">
                      {p.status}
                    </Pill>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-[var(--color-text-muted)] py-2 text-center">
                No preventive protocol coverage data.
              </p>
            )}
          </Panel>

          {/* 6. Treatment Plans & Prescriptions */}
          {recordIdShown != null && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-1.5">
                  <Icon name="pill" size={16} className="text-[var(--color-primary)]" />
                  Treatment Plans &amp; Prescriptions ({plans.length})
                </h2>
                <Button
                  variant="secondary"
                  size="sm"
                  icon="plus"
                  onClick={() => setShowPlanModal(true)}
                >
                  Add Treatment Plan
                </Button>
              </div>

              {plans.length === 0 ? (
                <Panel padded className="text-center py-6 text-sm text-[var(--color-text-muted)]">
                  No active treatment plans assigned to this record.
                </Panel>
              ) : (
                <div className="space-y-3">
                  {plans.map((p) => (
                    <Panel key={p.id} padded className="space-y-2.5 border-l-4 border-l-[var(--color-primary)]">
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border)] pb-2">
                        <div>
                          <span className="font-bold text-sm text-[var(--color-text-primary)]">
                            {p.treatmentName}
                          </span>
                          <span className="text-xs text-[var(--color-text-muted)] ml-2">
                            ({formatDate(p.startDate)} – {formatDate(p.endDate)})
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Pill tone="info" size="sm">
                            {p.status}
                          </Pill>
                          <Button
                            variant="tertiary"
                            size="sm"
                            icon="plus"
                            onClick={() =>
                              setActivePlanForPrescription({ id: p.id, name: p.treatmentName })
                            }
                          >
                            Prescribe Med
                          </Button>
                        </div>
                      </div>

                      <p className="text-sm text-[var(--color-text-secondary)]">
                        {p.instructions}
                      </p>

                      {p.prescriptions && p.prescriptions.length > 0 && (
                        <div className="mt-2.5 pt-2 border-t border-[var(--color-border)]">
                          <span className="text-[11px] font-semibold text-[var(--color-text-muted)] uppercase block mb-1.5">
                            Prescribed Medications:
                          </span>
                          <div className="space-y-1.5">
                            {p.prescriptions.map((rx) => (
                              <div
                                key={rx.id}
                                className="flex flex-wrap items-center justify-between text-xs bg-[var(--color-surface-muted)] px-2.5 py-1.5 rounded"
                              >
                                <div>
                                  <strong className="text-[var(--color-text-primary)]">
                                    {rx.medicationName}
                                  </strong>{' '}
                                  <span className="text-[var(--color-text-secondary)]">
                                    — {rx.dosage} ({rx.route}), {rx.frequency}
                                  </span>
                                </div>
                                <span className="text-[var(--color-text-muted)]">
                                  {formatDate(rx.startDate)} - {formatDate(rx.endDate)}
                                </span>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}
                    </Panel>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* 7. Health metrics trend chart */}
          {metrics.length > 0 && (
            <Panel padded className="space-y-3">
              <h2 className="text-sm font-bold text-[var(--color-text-primary)] flex items-center gap-1.5">
                <Icon name="activity" size={16} className="text-[var(--color-primary)]" />
                Health Metrics Trend
              </h2>
              <div className="h-48">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={metrics.map((m) => ({ ...m, date: formatDate(m.recordedAt) }))}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" tick={{ fontSize: 10 }} />
                    <YAxis tick={{ fontSize: 10 }} />
                    <Tooltip />
                    <Legend />
                    <Line type="monotone" dataKey="heartRate" stroke="var(--color-danger)" name="Heart rate" dot={false} />
                    <Line type="monotone" dataKey="temperature" stroke="var(--color-warning)" name="Temp °C" dot={false} />
                    <Line type="monotone" dataKey="weight" stroke="var(--color-info)" name="Weight kg" dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </Panel>
          )}
        </div>
      )}

      {/* 3D injury map modal — opened from the Injury section button */}
      {showInjuryMap && (
        <Modal
          open={showInjuryMap}
          onClose={() => setShowInjuryMap(false)}
          title={`3D Injury Map — ${resolvedHorseName}`}
          description="Red dots mark injury locations. Select a tag to toggle the condition details."
          size="lg"
          footer={
            <div className="flex justify-end">
              <Button variant="secondary" onClick={() => setShowInjuryMap(false)}>
                Close
              </Button>
            </div>
          }
        >
          <UrgentHorseInjuryViewer injuries={markerData} isAddMode={false} readOnly />
        </Modal>
      )}

      {/* Sub-modals */}
      {recordIdShown != null && (
        <TreatmentPlanModal
          open={showPlanModal}
          onClose={() => setShowPlanModal(false)}
          healthRecordId={recordIdShown}
          horseName={resolvedHorseName}
          onSuccess={() => {
            void load();
            onScheduleUpdated?.();
          }}
        />
      )}

      {activePlanForPrescription && (
        <PrescriptionModal
          open={Boolean(activePlanForPrescription)}
          onClose={() => setActivePlanForPrescription(null)}
          treatmentPlanId={activePlanForPrescription.id}
          treatmentName={activePlanForPrescription.name}
          onSuccess={() => {
            void load();
            onScheduleUpdated?.();
          }}
        />
      )}
    </div>
  );
}
