import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { HorseDashboardView } from '@/features/dashboard/components/HorseDashboardView';

export const metadata: Metadata = {
  title: 'Dashboard | Owner | RTMS',
};

export default function OwnerPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <PageContainer>
        <HorseDashboardView title="My horses overview" horseBasePath="/owner/horses" showHealth />
      </PageContainer>
    </RoleGuard>
  );
}
