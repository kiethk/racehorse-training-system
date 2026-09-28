import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { StaffManagementView } from '@/features/staff/components/StaffManagementView';

export const metadata: Metadata = {
  title: 'Staff Management | Manager',
};

export default function ManagerStaffPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          <StaffManagementView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
