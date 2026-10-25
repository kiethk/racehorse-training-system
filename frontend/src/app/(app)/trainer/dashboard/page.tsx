import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { HorseDashboardView } from '@/features/dashboard/components/HorseDashboardView';

export const metadata: Metadata = {
  title: 'Trainer dashboard | RTMS',
};

export default function TrainerDashboardPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <HorseDashboardView title="Fitness and training overview" horseBasePath="/trainer/horses" />
      </PageContainer>
    </RoleGuard>
  );
}
