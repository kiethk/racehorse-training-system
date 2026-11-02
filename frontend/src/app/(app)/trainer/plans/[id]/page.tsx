import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { GroupPlanDetail } from '@/features/training/components/GroupPlanDetail';

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
        <GroupPlanDetail planId={planId} />
      </PageContainer>
    </RoleGuard>
  );
}
