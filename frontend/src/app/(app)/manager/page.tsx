import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagerDashboardView } from '@/features/dashboard/components/manager/ManagerDashboardView';

export const metadata: Metadata = {
  title: 'Executive Dashboard | Manager | RTMS',
};

export default function ManagerPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <ManagerDashboardView />
      </PageContainer>
    </RoleGuard>
  );
}
