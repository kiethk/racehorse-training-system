import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { StableMap } from '@/features/stable/components/StableMap';

export const metadata: Metadata = {
  title: 'Stable map and groom assignments | Trainer | RTMS',
};

export default function TrainerStablePage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <StableMap />
      </PageContainer>
    </RoleGuard>
  );
}
