'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { Button } from '@/components/ui/Button';
import { DetailSkeleton } from '@/components/ui/states';
import { admissionsApi } from '@/features/admissions/services/api';
import type { CareScheduleDetail } from '@/features/admissions/types';
import { UrgentCaseView } from '@/features/admissions/components/UrgentCaseView';
import { VetReviewForm } from '@/features/admissions/components/VetReviewForm';

export default function ScheduleDetailPage() {
  const params = useParams();
  const router = useRouter();
  const scheduleId = Number(params?.scheduleId);
  const [detail, setDetail] = useState<CareScheduleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    const load = async () => {
      try {
        setLoading(true);
        const res = await admissionsApi.getCareScheduleDetail(scheduleId);
        if (!ignore) setDetail(res);
      } catch (err: unknown) {
        if (!ignore) {
          const msg = err instanceof Error ? err.message : 'Failed to load schedule';
          setError(msg);
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    if (!Number.isNaN(scheduleId)) load();
    return () => {
      ignore = true;
    };
  }, [scheduleId]);

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <div className="mb-4">
            <Button
              variant="secondary"
              size="sm"
              icon="arrow-left"
              onClick={() => router.push('/veterinarian/schedule')}
              className="transition-all active:scale-[0.98]"
            >
              Back to Schedule Timeline
            </Button>
          </div>
          {loading ? (
            <DetailSkeleton />
          ) : error ? (
            <div className="p-4 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
          ) : detail?.schedule ? (
            detail.schedule.careType === 'URGENT' ? (
              <UrgentCaseView scheduleId={scheduleId} />
            ) : (
              <VetReviewForm
                admissionId={detail.schedule.admissionId ?? 0}
                careScheduleId={detail.schedule.id}
                careType={detail.schedule.careType}
                scheduleStatus={detail.schedule.status}
                horseId={detail.schedule.horseId ?? undefined}
                onSuccess={() => router.push('/veterinarian/schedule')}
              />
            )
          ) : null}
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
