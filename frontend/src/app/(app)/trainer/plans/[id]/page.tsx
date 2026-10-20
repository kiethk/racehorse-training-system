import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { PlanDetail } from '@/features/training/components/PlanDetail';

export const metadata: Metadata = {
  title: 'Training plan details | Trainer | RTMS',
};

export default async function TrainingPlanDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = await params;
  const planId = Number(resolvedParams.id);

  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <PlanDetail planId={planId} />
      </PageContainer>
    </RoleGuard>
  );
}
