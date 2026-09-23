import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagerQueueList } from '@/features/admissions/components/ManagerQueueList';

export const metadata: Metadata = {
  title: 'Admissions | Manager',
};

export default function ManagerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <ManagerQueueList />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
