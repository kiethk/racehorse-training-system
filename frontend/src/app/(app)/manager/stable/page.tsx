import { Suspense } from 'react';
import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ListSkeleton } from '@/components/ui';
import { ManagerStableView } from '@/features/stable/components/ManagerStableView';

export const metadata: Metadata = {
  title: 'Stable | Manager | RTMS',
};

export default function ManagerStablePage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <Suspense fallback={<ListSkeleton rows={4} />}>
          <ManagerStableView />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
