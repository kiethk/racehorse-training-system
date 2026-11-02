import { Metadata } from 'next';
import { Suspense } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { GroupPlanWizard } from '@/features/training/components/GroupPlanWizard';

export const metadata: Metadata = {
  title: 'Create plan | Trainer | RTMS',
};

export default function NewTrainingPlanPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <Suspense fallback={<div className="p-4">Loading…</div>}>
          <GroupPlanWizard />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
