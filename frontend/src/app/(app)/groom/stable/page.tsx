import { Metadata } from 'next';
import { Suspense } from 'react';
import { LoadingScreen } from '@/components/ui';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { GroomStableView } from '@/features/stable/components/GroomStableView';

export const metadata: Metadata = {
  title: 'Stable | Groom | RTMS',
};

export default function GroomStablePage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        <Suspense fallback={<LoadingScreen />}><GroomStableView /></Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
