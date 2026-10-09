import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { PlanList } from '@/features/training/components/PlanList';

export const metadata: Metadata = {
  title: 'Training plans | Trainer | RTMS',
};

export default function TrainerPlansPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <PlanList />
      </PageContainer>
    </RoleGuard>
  );
}
