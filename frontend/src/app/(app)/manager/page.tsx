import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { HorseDashboardView } from '@/features/dashboard/components/HorseDashboardView';

export const metadata: Metadata = {
  title: 'Dashboard | Manager | RTMS',
};

export default function ManagerPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <HorseDashboardView title="Club overview" horseBasePath="/manager/horses" showHealth />
      </PageContainer>
    </RoleGuard>
  );
}
