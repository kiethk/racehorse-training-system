'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Button,
  DetailSkeleton,
  EmptyState,
  LinkButton,
  Notice,
  PageHeader,
  Panel,
  Pill,
  ScreenLayout,
} from '@/components/ui';
import { displayError, formatDateTime, formatEnumLabel } from '@/lib/display';
import { toast } from '@/lib/toast';
import { admissionsApi } from '@/features/admissions/services/api';
import { VetReviewForm } from '@/features/admissions/components/VetReviewForm';
import { AdmissionSideCard, InfoRow } from '@/features/admissions/shared/components/AdmissionInfoSection';
import type { CareScheduleDetail } from '@/features/admissions/types';

const BACK_HREF = '/veterinarian/preventive-care';

/**
 * Exam workspace for a care schedule that is not part of an admission, such as a
 * preventive vaccination milestone. Admission and urgent schedules keep their own screens.
 */
export function CareScheduleExamView({ scheduleId }: { scheduleId: number }) {
  const router = useRouter();
  const [detail, setDetail] = useState<CareScheduleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [starting, setStarting] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const data = await admissionsApi.getCareScheduleDetail(scheduleId);
      if (data.schedule.admissionId) {
        router.replace(`/veterinarian/admissions?scheduleId=${scheduleId}`);
        return;
      }
      if (data.schedule.careType === 'URGENT') {
        router.replace(`/veterinarian/urgent/${scheduleId}`);
        return;
      }
      setDetail(data);
    } catch (cause) {
      setError(displayError(cause, 'Could not load this examination.'));
    } finally {
      setLoading(false);
    }
  }, [router, scheduleId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function startExam() {
    if (!detail) return;
    setStarting(true);
    setError(null);
    try {
      const schedule = await admissionsApi.startCareSchedule(detail.schedule.id);
      setDetail((current) => (current ? { ...current, schedule: { ...current.schedule, status: schedule.status } } : current));
    } catch (cause) {
      setError(displayError(cause, 'Could not start the examination.'));
    } finally {
      setStarting(false);
    }
  }

  if (loading) return <DetailSkeleton />;
  if (!detail) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Examination unavailable"
          description={error ?? 'This examination could not be found.'}
          action={<LinkButton href={BACK_HREF} icon="chevron-left">Back to preventive care</LinkButton>}
        />
      </Panel>
    );
  }

  const { schedule, horse, healthRecord } = detail;
  const horseName = horse?.name ?? `Horse #${schedule.horseId}`;
  const preventive = schedule.purpose === 'PREVENTIVE';
  const visitLabel = preventive ? 'Vaccination visit' : `${formatEnumLabel(schedule.careType)} examination`;
  const canExam = schedule.status === 'SCHEDULED' || schedule.status === 'IN_PROGRESS';

  const detailsCard = (
    <AdmissionSideCard title="Visit details">
      <InfoRow label="Horse" value={`${horseName} · #${schedule.horseId}`} />
      <InfoRow label="Breed" value={horse?.breed} />
      <InfoRow label="Type" value={<Pill tone={preventive ? 'info' : 'neutral'} size="sm">{preventive ? 'Vaccination' : 'Follow-up'}</Pill>} />
      <InfoRow label="Status" value={formatEnumLabel(schedule.status)} />
      <InfoRow label="Scheduled" value={schedule.scheduledAt ? formatDateTime(schedule.scheduledAt) : null} />
      {schedule.description && <InfoRow label="Reason" value={schedule.description} />}
      {schedule.status === 'SCHEDULED' && (
        <Button className="w-full" variant="primary" icon="activity" loading={starting} onClick={startExam}>
          Start examination
        </Button>
      )}
    </AdmissionSideCard>
  );

  return (
    <ScreenLayout variant="detail">
      <PageHeader
        breadcrumbs={[{ label: 'Preventive Care', href: BACK_HREF }, { label: `Visit #${schedule.id}` }]}
        eyebrow={visitLabel}
        title={horseName}
      />
      {error && <Notice tone="error" onDismiss={() => setError(null)}>{error}</Notice>}

      {canExam ? (
        <VetReviewForm
          key={schedule.id}
          horseId={schedule.horseId}
          candidateName={horseName}
          careScheduleId={schedule.id}
          careType={schedule.careType}
          scheduleStatus={schedule.status}
          schedulePurpose={schedule.purpose}
          onStartExam={startExam}
          asideTop={detailsCard}
          onSuccess={() => {
            toast.success('Examination completed', `${horseName}'s health record has been updated.`);
            setLoading(true);
            load();
          }}
        />
      ) : (
        <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_20rem]">
          <Panel padded className="space-y-3">
            {schedule.status === 'COMPLETED' ? (
              <>
                <Notice tone="success" title="Examination completed">
                  Completed {schedule.completedAt ? formatDateTime(schedule.completedAt) : ''}.
                </Notice>
                {healthRecord && (
                  <div className="space-y-3 text-sm">
                    <InfoRow label="Diagnosis" value={healthRecord.diagnosis} />
                    <InfoRow label="Findings" value={healthRecord.findings} />
                    <InfoRow label="Treatment" value={healthRecord.treatment} />
                    <InfoRow
                      label="Training decision"
                      value={
                        <Pill tone={healthRecord.trainingDecision === 'ALLOWED' ? 'success' : 'danger'} size="sm">
                          {formatEnumLabel(healthRecord.trainingDecision)}
                        </Pill>
                      }
                    />
                    {healthRecord.restrictionDetails && (
                      <InfoRow label="Restriction" value={healthRecord.restrictionDetails} />
                    )}
                  </div>
                )}
              </>
            ) : (
              <EmptyState
                icon="clock"
                title={schedule.status === 'REQUESTED' ? 'Waiting for a veterinarian' : 'Examination closed'}
                description={
                  schedule.status === 'REQUESTED'
                    ? 'This visit is waiting for automatic assignment and cannot be started yet.'
                    : `This visit is ${formatEnumLabel(schedule.status).toLowerCase()} and can no longer be examined.`
                }
              />
            )}
            <LinkButton href={BACK_HREF} variant="secondary" icon="chevron-left">Back to preventive care</LinkButton>
          </Panel>
          {detailsCard}
        </div>
      )}
    </ScreenLayout>
  );
}
