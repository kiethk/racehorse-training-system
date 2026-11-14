import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { CourseRunDetail } from '@/features/training/components/course-runs/CourseRunDetail';

export const metadata: Metadata = {
  title: 'Course run details | Trainer | RTMS',
};

export default async function TrainingPlanDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = await params;
  const courseRunId = Number(resolvedParams.id);

  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <CourseRunDetail courseRunId={courseRunId} />
      </PageContainer>
    </RoleGuard>
  );
}
