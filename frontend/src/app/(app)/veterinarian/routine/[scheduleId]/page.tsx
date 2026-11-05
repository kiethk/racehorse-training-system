'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { BackLink } from '@/components/ui';
import { Button } from '@/components/ui/Button';
import { Spinner } from '@/components/ui';
import { DetailSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import type { CareScheduleDetail } from '@/features/admissions/types';
import { VetReviewForm } from '@/features/admissions/components/VetReviewForm';
import { SchedulePatientHeader } from '@/features/care/components/SchedulePatientHeader';

export default function RoutineDetailPage() {
  const params = useParams();
  const router = useRouter();
  const scheduleId = Number(params?.scheduleId);
  const [detail, setDetail] = useState<CareScheduleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  // The list card already asked to start the exam; entering this page auto-starts it once.
  const autoStartRef = useRef(false);

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

  const handleStartExam = useCallback(async () => {
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
  }, [scheduleId]);

  const isCompleted = detail?.schedule?.status === 'COMPLETED';

  // Completed schedules share the unified View Record screen (Routine/Health/Schedule).
  useEffect(() => {
    if (isCompleted && !Number.isNaN(scheduleId)) {
      router.replace(`/veterinarian/records?scheduleId=${scheduleId}`);
    }
  }, [isCompleted, scheduleId, router]);

  // Auto-start a SCHEDULED exam on entry; on failure the gated form keeps the manual button.
  useEffect(() => {
    if (detail?.schedule?.status === 'SCHEDULED' && !autoStartRef.current) {
      autoStartRef.current = true;
      handleStartExam().catch(() => {
        // The form stays gated; the vet can still use the manual Start Examination button.
      });
    }
  }, [detail, handleStartExam]);

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <div className="mb-4">
          <BackLink onClick={() => router.push('/veterinarian/routine')}>Back to Routine Care</BackLink>
        </div>

        {loading ? (
          <DetailSkeleton />
        ) : error && !detail?.schedule ? (
          <div className="p-4 rounded-lg bg-red-50 text-red-700 text-sm flex items-center justify-between">
            <span>{error}</span>
            <Button variant="secondary" size="sm" onClick={loadDetail}>
              Retry
            </Button>
          </div>
        ) : detail?.schedule ? (
          isCompleted ? (
            <div className="flex justify-center py-16">
              <Spinner size="lg" />
            </div>
          ) : (
            <div className="space-y-4">
              <SchedulePatientHeader
                scheduleId={detail.schedule.id}
                status={detail.schedule.status}
                careType={detail.schedule.careType}
                purpose={detail.schedule.purpose}
                scheduledAt={detail.schedule.scheduledAt ?? detail.schedule.scheduledDate}
                horseName={detail.horse?.name}
                horseBreed={detail.horse?.breed}
                veterinarianName={detail.veterinarian?.fullName}
              />
              {error && (
                <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
              )}
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
            </div>
          )
        ) : null}
      </PageContainer>
    </RoleGuard>
  );
}
