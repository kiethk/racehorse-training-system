import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerDashboardView } from '@/features/training/components/TrainerDashboardView';

export const metadata: Metadata = {
  title: 'Trainer dashboard | RTMS',
};

export default function TrainerDashboardPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainerDashboardView />
      </PageContainer>
    </RoleGuard>
  );
}
