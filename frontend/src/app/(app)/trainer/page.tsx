import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { HorseDashboardView } from '@/features/dashboard/components/HorseDashboardView';

export const metadata: Metadata = {
  title: 'Dashboard | Trainer | RTMS',
};

export default function TrainerPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <HorseDashboardView title="Fitness and training overview" horseBasePath="/trainer/horses" />
      </PageContainer>
    </RoleGuard>
  );
}
