import { Metadata } from 'next';
import { Suspense } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { CourseRunWizard } from '@/features/training/components/course-runs/CourseRunWizard';

export const metadata: Metadata = {
  title: 'Create course run | Trainer | RTMS',
};

export default function NewTrainingPlanPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <Suspense fallback={<div className="p-4">Loading…</div>}>
          <CourseRunWizard />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
