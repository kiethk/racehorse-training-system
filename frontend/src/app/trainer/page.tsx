'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerDashboardView } from '@/features/training/components/TrainerDashboardView';

export default function TrainerPage() {
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

