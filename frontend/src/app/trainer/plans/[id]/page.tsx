import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { PlanDetail } from '@/features/training/components/PlanDetail';

export const metadata: Metadata = {
  title: 'Chi tiết kế hoạch huấn luyện | Huấn luyện viên',
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
      <AppShell>
        <PageContainer>
          <PlanDetail planId={planId} />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
