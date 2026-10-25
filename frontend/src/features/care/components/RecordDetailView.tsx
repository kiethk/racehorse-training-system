'use client';

import { useCallback, useEffect, useState } from 'react';
import { BackLink } from '@/components/ui/BackLink';
import { Button, Icon, Notice, Panel, Pill, Spinner } from '@/components/ui';
import dynamic from 'next/dynamic';
import { careService } from '../services/careService';
import { healthApi } from '@/features/health/services/healthApi';
import type { HealthRecord } from '@/features/health/types';
import type { InjuryRecord } from '@/features/health/types';
import type { InjuryMarkerData } from '@/features/vet/components/UrgentHorseInjuryViewer';
import { zoneById } from '@/features/vet/lib/injuryZones';
import { TreatmentPlanModal } from '../shared/components/TreatmentPlanModal';
import { PrescriptionModal } from './PrescriptionModal';
import type { CareScheduleDetailResponse, TreatmentPlan } from '../types';
import type { HorseHealthMetric } from '@/features/horses/types';
import { displayError, formatDate, formatDateTime } from '@/lib/display';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

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
  const [plans, setPlans] = useState<TreatmentPlan[]>([]);
  const [metrics, setMetrics] = useState<HorseHealthMetric[]>([]);
  const [injuries, setInjuries] = useState<InjuryRecord[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [showPlanModal, setShowPlanModal] = useState(false);
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

  if (!loading && !error && !detail?.schedule && !fullRecord) {
    return <Notice tone="warning">No clinical record to display.</Notice>;
  }

  const resolvedHorseId = detail?.schedule?.horseId ?? fullRecord?.horseId ?? null;
  const recordIdShown = detail?.healthRecord?.id ?? fullRecord?.id ?? null;
  // Only injuries actually captured on the 3D model have coordinates; the rest
  // stay in the tag list without a marker instead of being dropped at origin.
  const markerData: InjuryMarkerData[] = injuries
    .filter((i) => i.positionX != null)
    .map((i) => ({
      id: String(i.id),
      position: [i.positionX ?? 0, i.positionY ?? 0, i.positionZ ?? 0],
      zoneId: i.zoneId ?? undefined,
      label: (i.zoneId && zoneById(i.zoneId)?.label) || i.injuryType,
    }));

  return (
    <div className="space-y-4">
      {!onHideBack && (
        <BackLink
          href={backHref}
          label={
            backHref.includes('health?')
              ? 'Back to health profile'
              : backHref.includes('routine')
                ? 'Back to Routine Care'
                : 'Back to dashboard'
          }
        />
      )}

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
          {/* Header metadata summary */}
          <div className="grid grid-cols-2 gap-3 p-3 sm:grid-cols-4 rounded-[var(--radius-sm)] bg-[var(--color-surface-muted)] text-[12px]">
            <div>
              <span className="text-[var(--color-text-muted)] block">Horse:</span>
              <span className="font-semibold text-[var(--color-text-primary)]">
                {detail?.horse?.name || horseName || (resolvedHorseId != null ? `#${resolvedHorseId}` : '—')}
              </span>
            </div>
            {detail?.schedule && (
              <>
                <div>
                  <span className="text-[var(--color-text-muted)] block">Care Type:</span>
                  <span className="font-medium text-[var(--color-text-primary)]">
                    {detail.schedule.careType}
                  </span>
                </div>
                <div>
                  <span className="text-[var(--color-text-muted)] block">Status:</span>
                  <Pill
                    tone={
                      detail.schedule.status === 'COMPLETED'
                        ? 'success'
                        : detail.schedule.status === 'IN_PROGRESS'
                        ? 'warning'
                        : 'info'
                    }
                    size="sm"
                  >
                    {detail.schedule.status}
                  </Pill>
                </div>
              </>
            )}
            <div>
              <span className="text-[var(--color-text-muted)] block">Veterinarian:</span>
              <span className="font-medium text-[var(--color-text-primary)]">
                {detail?.veterinarian?.fullName || 'Assigned Staff'}
              </span>
            </div>
          </div>

          {/* Health Record Section */}
          {recordIdShown ? (
            <Panel padded className="space-y-3">
              <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border-subtle)] pb-2">
                <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] flex items-center gap-1.5">
                  <Icon name="clipboard" size={16} className="text-[var(--color-primary)]" />
                  Clinical Record #{recordIdShown}
                  {fullRecord?.recordType && (
                    <Pill tone="neutral" size="sm">
                      {fullRecord.recordType}
                    </Pill>
                  )}
                </h2>
                <div className="flex items-center gap-2">
                  {fullRecord?.examinedAt && (
                    <span className="text-[12px] text-[var(--color-text-muted)]">
                      {formatDateTime(fullRecord.examinedAt)}
                    </span>
                  )}
                  <Pill
                    tone={
                      (detail?.healthRecord?.trainingDecision ?? fullRecord?.trainingDecision) ===
                      'BLOCKED'
                        ? 'danger'
                        : 'success'
                    }
                    size="sm"
                  >
                    Training: {detail?.healthRecord?.trainingDecision ?? fullRecord?.trainingDecision}
                  </Pill>
                </div>
              </div>

              <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 text-[13px]">
                <div>
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Symptoms:
                  </span>
                  <p className="text-[var(--color-text-primary)]">
                    {detail?.healthRecord?.symptoms || fullRecord?.symptoms || 'None reported.'}
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
                <div className="text-[13px]">
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Product / Service:
                  </span>
                  <p className="text-[var(--color-text-secondary)]">{fullRecord.productOrService}</p>
                </div>
              )}

              {fullRecord?.notes && (
                <div className="text-[13px]">
                  <span className="text-[var(--color-text-muted)] text-[11px] uppercase font-semibold block">
                    Notes:
                  </span>
                  <p className="text-[var(--color-text-secondary)]">{fullRecord.notes}</p>
                </div>
              )}

              {(detail?.healthRecord?.restrictionDetails || fullRecord?.restrictionDetails) && (
                <Notice tone="warning" title="Training Restriction Details">
                  {detail?.healthRecord?.restrictionDetails || fullRecord?.restrictionDetails}
                </Notice>
              )}
            </Panel>
          ) : (
            <Panel padded className="text-[13px] text-[var(--color-text-muted)] italic">
              No clinical health record has been completed for this schedule yet.
            </Panel>
          )}

          {/* Injury map (3D) with per-injury tag list */}
          {resolvedHorseId != null && (
            <Panel padded className="space-y-3">
              <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] flex items-center gap-1.5">
                <Icon name="alert-triangle" size={16} className="text-[var(--color-danger)]" />
                Injury Map (3D)
              </h2>
              <UrgentHorseInjuryViewer injuries={markerData} isAddMode={false} readOnly />

              {injuries.length === 0 ? (
                <p className="text-xs text-[var(--color-text-muted)] py-2 text-center">
                  No injury records for this horse.
                </p>
              ) : (
                <div className="space-y-1.5">
                  {injuries.map((inj) => {
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
                            <Pill tone={inj.status === 'ACTIVE' ? 'danger' : 'success'} size="sm">
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
          )}

          {/* Treatment Plans & Prescriptions */}
          {recordIdShown != null && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] flex items-center gap-1.5">
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
                <Panel padded className="text-center py-6 text-[13px] text-[var(--color-text-muted)]">
                  No active treatment plans assigned to this record.
                </Panel>
              ) : (
                <div className="space-y-3">
                  {plans.map((p) => (
                    <Panel key={p.id} padded className="space-y-2.5 border-l-4 border-l-[var(--color-primary)]">
                      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--color-border-subtle)] pb-2">
                        <div>
                          <span className="font-bold text-[14px] text-[var(--color-text-primary)]">
                            {p.treatmentName}
                          </span>
                          <span className="text-[12px] text-[var(--color-text-muted)] ml-2">
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

                      <p className="text-[13px] text-[var(--color-text-secondary)]">
                        {p.instructions}
                      </p>

                      {p.prescriptions && p.prescriptions.length > 0 && (
                        <div className="mt-2.5 pt-2 border-t border-[var(--color-border-subtle)]">
                          <span className="text-[11px] font-semibold text-[var(--color-text-muted)] uppercase block mb-1.5">
                            Prescribed Medications:
                          </span>
                          <div className="space-y-1.5">
                            {p.prescriptions.map((rx) => (
                              <div
                                key={rx.id}
                                className="flex flex-wrap items-center justify-between text-[12px] bg-[var(--color-surface-muted)] px-2.5 py-1.5 rounded"
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

          {/* Health metrics + trend chart */}
          {metrics.length > 0 && (
            <Panel padded className="space-y-3">
              <h2 className="text-[14px] font-semibold text-[var(--color-text-primary)] flex items-center gap-1.5">
                <Icon name="activity" size={16} className="text-[var(--color-primary)]" />
                Health Metrics &amp; Trend
              </h2>
              <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 text-[12px]">
                {(() => {
                  const last = metrics[metrics.length - 1];
                  return (
                    <>
                      <div>
                        <span className="text-[var(--color-text-muted)] block">Heart rate</span>
                        <span className="font-semibold">{last.heartRate ?? '—'}</span>
                      </div>
                      <div>
                        <span className="text-[var(--color-text-muted)] block">Temp (°C)</span>
                        <span className="font-semibold">{last.temperature ?? '—'}</span>
                      </div>
                      <div>
                        <span className="text-[var(--color-text-muted)] block">Weight (kg)</span>
                        <span className="font-semibold">{last.weight ?? '—'}</span>
                      </div>
                      <div>
                        <span className="text-[var(--color-text-muted)] block">Resp. rate</span>
                        <span className="font-semibold">{last.respiratoryRate ?? '—'}</span>
                      </div>
                    </>
                  );
                })()}
              </div>
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

      {/* Sub-modals */}
      {recordIdShown != null && (
        <TreatmentPlanModal
          open={showPlanModal}
          onClose={() => setShowPlanModal(false)}
          healthRecordId={recordIdShown}
          horseName={detail?.horse?.name ?? horseName}
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
