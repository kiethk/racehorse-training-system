import type { Metadata } from 'next';
import { Suspense } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { AccountManagementView } from '@/features/accounts/components/AccountManagementView';
import { ListSkeleton } from '@/components/ui/states';

export const metadata: Metadata = {
  title: 'Accounts | RTMS Manager',
  description: 'Manage staff, horse owners, and administrator accounts across the system.',
};

export default function AccountsPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <Suspense fallback={<div className="p-6"><ListSkeleton rows={8} /></div>}>
          <AccountManagementView />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
