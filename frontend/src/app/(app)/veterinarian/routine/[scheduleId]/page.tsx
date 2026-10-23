'use client';

import { useCallback, useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { Icon } from '@/components/ui/Icon';
import { DetailSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import type { CareScheduleDetail } from '@/features/admissions/types';
import { VetReviewForm } from '@/features/admissions/components/VetReviewForm';

function formatDateTime(value?: string | null): string {
  if (!value) return 'Not recorded';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return value;
  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(d);
}

export default function RoutineDetailPage() {
  const params = useParams();
  const router = useRouter();
  const scheduleId = Number(params?.scheduleId);
  const [detail, setDetail] = useState<CareScheduleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadDetail = useCallback(async () => {
    if (Number.isNaN(scheduleId)) return;
    try {
      setLoading(true);
      setError(null);
      const res = await admissionsApi.getCareScheduleDetail(scheduleId);
      setDetail(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to load routine schedule';
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [scheduleId]);

  useEffect(() => {
    let active = true;
    Promise.resolve().then(() => {
      if (active) void loadDetail();
    });
    return () => {
      active = false;
    };
  }, [loadDetail]);

  const handleStartExam = async () => {
    try {
      setError(null);
      await admissionsApi.startCareSchedule(scheduleId);
      const res = await admissionsApi.getCareScheduleDetail(scheduleId);
      setDetail(res);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to start examination';
      setError(msg);
      throw err;
    }
  };

  const isCompleted = detail?.schedule?.status === 'COMPLETED';
  const trainingDecision =
    detail?.healthRecord?.trainingDecision || detail?.horse?.trainingDecision || 'ALLOWED';

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <div className="mb-4">
          <Button
            variant="secondary"
            size="sm"
            icon="arrow-left"
            onClick={() => router.push('/veterinarian/routine')}
            className="transition-all active:scale-[0.98]"
          >
            Back to Routine Care
          </Button>
        </div>

        {loading ? (
          <DetailSkeleton />
        ) : error ? (
          <div className="p-4 rounded-lg bg-red-50 text-red-700 text-sm flex items-center justify-between">
            <span>{error}</span>
            <Button variant="secondary" size="sm" onClick={loadDetail}>
              Retry
            </Button>
          </div>
        ) : detail?.schedule ? (
          isCompleted ? (
            <Panel padded className="space-y-6">
              {/* Header with Title, Status & Back Button */}
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between border-b border-[var(--color-border)] pb-4">
                <div className="flex items-center gap-3">
                  <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--color-success-soft)] text-[var(--color-success)]">
                    <Icon name="check" size={20} />
                  </div>
                  <div>
                    <h2 className="text-lg font-bold text-[var(--color-text-primary)]">
                      Clinical Examination Summary
                    </h2>
                    <p className="text-xs text-[var(--color-text-secondary)]">
                      Routine Care Schedule #{detail.schedule.id} · Completed Examination
                    </p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Pill tone="success" icon="check" size="md">
                    Completed
                  </Pill>
                  <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => router.push('/veterinarian/routine')}
                  >
                    Back to Routine Care
                  </Button>
                </div>
              </div>

              {/* Patient & Vet Context Grid */}
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-4 text-xs">
                <div>
                  <span className="font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                    Patient Horse
                  </span>
                  <p className="mt-1 font-bold text-sm text-[var(--color-text-primary)]">
                    {detail.horse?.name || `Horse #${detail.schedule.horseId}`}
                  </p>
                  {detail.horse?.breed && (
                    <p className="text-[var(--color-text-secondary)]">{detail.horse.breed}</p>
                  )}
                </div>

                <div>
                  <span className="font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                    Attending Veterinarian
                  </span>
                  <p className="mt-1 font-bold text-sm text-[var(--color-text-primary)]">
                    {detail.veterinarian?.fullName || 'Assigned Veterinarian'}
                  </p>
                  {detail.veterinarian?.email && (
                    <p className="text-[var(--color-text-secondary)]">{detail.veterinarian.email}</p>
                  )}
                </div>

                <div>
                  <span className="font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                    Completion Date
                  </span>
                  <p className="mt-1 font-bold text-sm text-[var(--color-text-primary)]">
                    {formatDateTime((detail.schedule as { completedAt?: string | null }).completedAt || detail.schedule.scheduledAt)}
                  </p>
                  {detail.schedule.durationMinutes && (
                    <p className="text-[var(--color-text-secondary)]">
                      Duration: {detail.schedule.durationMinutes} min
                    </p>
                  )}
                </div>

                <div>
                  <span className="font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                    Training Decision
                  </span>
                  <div className="mt-1">
                    <Pill
                      tone={trainingDecision === 'BLOCKED' ? 'danger' : 'success'}
                      icon={trainingDecision === 'BLOCKED' ? 'lock' : 'check'}
                      size="sm"
                    >
                      {trainingDecision === 'BLOCKED' ? 'Training Blocked' : 'Training Allowed'}
                    </Pill>
                  </div>
                </div>
              </div>

              {/* Clinical Findings & Diagnosis */}
              <div className="space-y-4">
                <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-muted)] mb-1">
                    Clinical Diagnosis
                  </h3>
                  <p className="text-sm font-semibold text-[var(--color-text-primary)]">
                    {detail.healthRecord?.diagnosis || 'Clinically healthy'}
                  </p>
                </div>

                <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-muted)] mb-1">
                    Examination Findings
                  </h3>
                  <p className="text-sm whitespace-pre-wrap leading-relaxed text-[var(--color-text-secondary)]">
                    {detail.healthRecord?.findings || 'No specific findings recorded.'}
                  </p>
                </div>

                {detail.healthRecord?.treatment && (
                  <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-[var(--color-text-muted)] mb-1">
                      Administered Treatment & Recommendations
                    </h3>
                    <p className="text-sm whitespace-pre-wrap leading-relaxed text-[var(--color-text-secondary)]">
                      {detail.healthRecord.treatment}
                    </p>
                  </div>
                )}

                {trainingDecision === 'BLOCKED' && detail.healthRecord?.restrictionDetails && (
                  <div className="rounded-[var(--radius-md)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)]/20 p-4">
                    <div className="flex items-center gap-2 text-[var(--color-danger)] font-bold text-xs uppercase tracking-wider mb-1">
                      <Icon name="lock" size={14} />
                      <span>Training Restrictions</span>
                    </div>
                    <p className="text-sm font-medium text-[var(--color-danger)]">
                      {detail.healthRecord.restrictionDetails}
                    </p>
                  </div>
                )}

                {detail.schedule.description && (
                  <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-xs">
                    <span className="font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">
                      Schedule Description / Notes
                    </span>
                    <p className="mt-1 text-[var(--color-text-secondary)]">
                      {detail.schedule.description}
                    </p>
                  </div>
                )}
              </div>

              {/* Return Button */}
              <div className="flex justify-end pt-2">
                <Button
                  variant="secondary"
                  onClick={() => router.push('/veterinarian/routine')}
                >
                  Return to Routine Care
                </Button>
              </div>
            </Panel>
          ) : (
            <VetReviewForm
              admissionId={detail.schedule.admissionId ?? 0}
              careScheduleId={detail.schedule.id}
              careType={detail.schedule.careType}
              scheduleStatus={detail.schedule.status}
              schedulePurpose={detail.schedule.purpose}
              candidateName={detail.horse?.name}
              horseId={detail.schedule.horseId ?? undefined}
              onStartExam={handleStartExam}
              onSuccess={() => router.push('/veterinarian/routine')}
            />
          )
        ) : null}
      </PageContainer>
    </RoleGuard>
  );
}
