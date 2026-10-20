'use client';

import { useEffect, useState, useCallback } from 'react';
import { Icon } from '@/components/ui/Icon';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { DetailSkeleton } from '@/components/ui/states';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '@/features/admissions/services/api';
import type {
  CareSchedule,
  CareScheduleDetail,
  AdmissionDetailResponse,
} from '@/features/admissions/types';
import { UrgentCaseView } from '@/features/admissions/components/UrgentCaseView';
import { VetReviewForm } from '@/features/admissions/components/VetReviewForm';

interface Props {
  schedule: CareSchedule | null;
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export function VetScheduleDrawer({ schedule, open, onClose, onSuccess }: Props) {
  const [startedScheduleId, setStartedScheduleId] = useState<number | null>(null);
  const [scheduleDetail, setScheduleDetail] = useState<CareScheduleDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [admissionDetail, setAdmissionDetail] = useState<AdmissionDetailResponse | null>(null);
  const [isDirty, setIsDirty] = useState(false);
  const [showCloseConfirm, setShowCloseConfirm] = useState(false);

  useEffect(() => {
    if (!open || !schedule) return;

    let active = true;
    Promise.resolve().then(() => {
      if (!active) return;
      setLoading(true);
      setError(null);
    });

    const loadData = async () => {
      try {
        const promises: Promise<unknown>[] = [
          admissionsApi.getCareScheduleDetail(schedule.id).then((cs) => {
            if (active) setScheduleDetail(cs);
          }).catch(() => null),
        ];

        if (schedule.careType !== 'URGENT' && schedule.admissionId) {
          promises.push(
            admissionsApi.getVetAdmissionDetail(schedule.admissionId).then((detail) => {
              if (active) setAdmissionDetail(detail);
            }).catch(() => null)
          );
        }

        await Promise.all(promises);
      } catch (err) {
        if (active) setError(err instanceof Error ? err.message : 'Failed to load schedule details.');
      } finally {
        if (active) setLoading(false);
      }
    };

    void loadData();

    return () => {
      active = false;
      setAdmissionDetail(null);
      setScheduleDetail(null);
      setError(null);
      setIsDirty(false);
    };
  }, [open, schedule]);

  const handleRequestClose = useCallback(() => {
    if (isDirty) {
      setShowCloseConfirm(true);
    } else {
      onClose();
    }
  }, [isDirty, onClose]);

  if (!open || !schedule) return null;

  const isStarted = startedScheduleId === schedule.id;
  const effectiveStatus = isStarted ? 'IN_PROGRESS' : schedule.status;

  return (
    <>
      <div className="fixed inset-0 z-40 flex justify-end bg-black/50 backdrop-blur-sm transition-opacity">
        <div
          role="dialog"
          aria-modal="true"
          className="relative flex h-full w-full max-w-3xl flex-col bg-[var(--color-surface)] shadow-2xl overflow-hidden"
        >
          {/* Header */}
          <div className="flex items-center justify-between border-b border-[var(--color-border)] px-6 py-4 bg-[var(--color-surface-subtle)]">
            <div className="flex items-center gap-2.5">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-[var(--color-primary-soft)] text-[var(--color-primary)]">
                <Icon name="stethoscope" size={18} />
              </span>
              <div>
                <h2 className="text-sm font-bold text-[var(--color-text-primary)]">
                  {schedule.careType === 'URGENT'
                    ? `Urgent Case #${schedule.id}`
                    : schedule.careType === 'ROUTINE'
                    ? `Routine / Follow-up #${schedule.id}`
                    : `Admission Exam #${schedule.id}`}
                </h2>
                {schedule.description && (
                  <p className="text-[11px] text-[var(--color-text-secondary)]">
                    {schedule.description}
                  </p>
                )}
              </div>
            </div>

            <Button variant="secondary" size="sm" icon="x" onClick={handleRequestClose}>
              Close
            </Button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6">
            {loading ? (
              <DetailSkeleton />
            ) : error ? (
              <div className="p-4 rounded-lg bg-red-50 text-red-700 dark:bg-red-950/40 dark:text-red-300 text-sm">
                {error}
              </div>
            ) : effectiveStatus === 'COMPLETED' ? (
              <div className="space-y-6">
                <div className="flex items-center justify-between rounded-[var(--radius-md)] border border-emerald-200 bg-emerald-50/70 p-4">
                  <div className="flex items-center gap-2">
                    <span className="flex h-7 w-7 items-center justify-center rounded-full bg-emerald-100 text-emerald-700">
                      <Icon name="check" size={16} />
                    </span>
                    <div>
                      <p className="text-xs font-bold text-emerald-800">Completed Examination Record</p>
                      <p className="text-[11px] text-emerald-600">
                        Attending Veterinarian: {scheduleDetail?.veterinarian?.fullName || 'Staff Veterinarian'}
                        {scheduleDetail?.veterinarian?.email ? ` (${scheduleDetail.veterinarian.email})` : ''}
                      </p>
                    </div>
                  </div>
                  {scheduleDetail?.healthRecord && (
                    <Pill
                      tone={scheduleDetail.healthRecord.trainingDecision === 'BLOCKED' ? 'danger' : 'success'}
                      size="sm"
                    >
                      {scheduleDetail.healthRecord.trainingDecision === 'BLOCKED' ? 'Training Blocked' : 'Cleared for Training'}
                    </Pill>
                  )}
                </div>

                {scheduleDetail?.healthRecord ? (
                  <div className="space-y-4 rounded-[var(--radius-md)] border border-[var(--color-border)] p-4 bg-[var(--color-surface)]">
                    {scheduleDetail.healthRecord.restrictionDetails && (
                      <div className="rounded-[var(--radius-sm)] border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                        <p className="font-semibold">Training Restriction Details:</p>
                        <p className="mt-1">{scheduleDetail.healthRecord.restrictionDetails}</p>
                      </div>
                    )}

                    <div>
                      <h4 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">Clinical Findings & Examination</h4>
                      <p className="mt-1 text-sm text-[var(--color-text-primary)] whitespace-pre-wrap">
                        {scheduleDetail.healthRecord.findings || 'No abnormalities detected'}
                      </p>
                    </div>

                    <div className="border-t border-[var(--color-border)] pt-3">
                      <h4 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">Clinical Diagnosis</h4>
                      <p className="mt-1 text-sm font-medium text-[var(--color-text-primary)]">
                        {scheduleDetail.healthRecord.diagnosis || 'Normal'}
                      </p>
                    </div>

                    {scheduleDetail.healthRecord.treatment && (
                      <div className="border-t border-[var(--color-border)] pt-3">
                        <h4 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-secondary)]">Treatment Plan & Medications</h4>
                        <p className="mt-1 text-sm text-[var(--color-text-primary)] whitespace-pre-wrap">
                          {scheduleDetail.healthRecord.treatment}
                        </p>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="p-4 rounded-lg bg-[var(--color-surface-subtle)] text-center text-xs text-[var(--color-text-muted)]">
                    No detailed examination record found for this schedule.
                  </div>
                )}
              </div>
            ) : schedule.careType === 'URGENT' ? (
              <UrgentCaseView scheduleId={schedule.id} />
            ) : (
              <VetReviewForm
                admissionId={schedule.admissionId || 0}
                horseId={schedule.horseId}
                candidateName={admissionDetail?.candidate?.name || `Horse #${schedule.horseId}`}
                quarantineStallCode={admissionDetail?.quarantineStallCode}
                careScheduleId={schedule.id}
                careType={schedule.careType}
                scheduleStatus={effectiveStatus}
                onStartExam={async () => {
                  await admissionsApi.startCareSchedule(schedule.id);
                  setStartedScheduleId(schedule.id);
                  onSuccess();
                }}
                onSuccess={() => {
                  onSuccess();
                  onClose();
                }}
                onDirtyChange={setIsDirty}
              />
            )}
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={showCloseConfirm}
        title="Discard unsaved changes?"
        description="You have unsaved clinical entries. Closing this drawer will discard your progress."
        confirmLabel="Discard & Close"
        cancelLabel="Continue Editing"
        tone="danger"
        onConfirm={() => {
          setShowCloseConfirm(false);
          setIsDirty(false);
          onClose();
        }}
        onCancel={() => setShowCloseConfirm(false)}
      />
    </>
  );
}
