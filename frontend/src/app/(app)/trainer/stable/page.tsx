import type { Metadata } from 'next';
import { Suspense } from 'react';
import { LoadingScreen } from '@/components/ui';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerStableView } from '@/features/stable/components/TrainerStableView';

export const metadata: Metadata = {
  title: 'Stable | Trainer | RTMS',
};

export default function TrainerStablePage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <Suspense fallback={<LoadingScreen />}><TrainerStableView /></Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
