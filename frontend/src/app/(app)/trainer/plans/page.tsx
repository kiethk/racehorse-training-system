import type { Metadata } from 'next';
import { Suspense } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { LoadingScreen } from '@/components/ui';
import { GroupPlanList } from '@/features/training/components/GroupPlanList';

export const metadata: Metadata = {
  title: 'Training plans | Trainer | RTMS',
};

export default function TrainerPlansPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <Suspense fallback={<LoadingScreen />}>
          <GroupPlanList />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
