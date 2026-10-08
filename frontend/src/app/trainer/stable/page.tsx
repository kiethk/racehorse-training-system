import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { StableMap } from '@/features/stable/components/StableMap';

export const metadata: Metadata = {
  title: 'Stable Map & Groom Assignments | RTMS',
};

export default function TrainerStablePage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <StableMap />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
