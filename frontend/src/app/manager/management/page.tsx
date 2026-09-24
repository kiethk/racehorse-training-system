import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagementTabs } from '@/features/management/components/ManagementTabs';

export const metadata: Metadata = {
  title: 'Management | Manager',
};

export default function ManagerManagementPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <ManagementTabs />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
