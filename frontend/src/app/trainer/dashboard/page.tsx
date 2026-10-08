import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerDashboardView } from '@/features/training/components/TrainerDashboardView';

export const metadata: Metadata = {
  title: 'Trainer dashboard | RTMS',
};

export default function TrainerDashboardPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerDashboardView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
