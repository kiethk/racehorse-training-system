import { Metadata } from 'next';
import { Suspense } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { EnrollmentForm } from '@/features/training/components/EnrollmentForm';

export const metadata: Metadata = {
  title: 'Group training enrolment | Trainer | RTMS',
};

export default function NewTrainingPlanPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <Suspense fallback={<div className="p-4">Loading…</div>}>
            <EnrollmentForm />
          </Suspense>
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
