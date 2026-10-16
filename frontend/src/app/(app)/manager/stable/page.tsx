import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagerStableView } from '@/features/stable/components/ManagerStableView';

export const metadata: Metadata = {
  title: 'Stable | Manager | RTMS',
};

export default function ManagerStablePage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <ManagerStableView />
      </PageContainer>
    </RoleGuard>
  );
}
