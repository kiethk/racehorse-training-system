import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { AccountManagementView } from '@/features/accounts/components/AccountManagementView';

export const metadata: Metadata = {
  title: 'Accounts | RTMS Manager',
  description: 'Manage staff, horse owners, and administrator accounts across the system.',
};

export default function AccountsPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <AccountManagementView />
      </PageContainer>
    </RoleGuard>
  );
}
