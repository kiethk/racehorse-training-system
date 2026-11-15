import type { Metadata } from 'next';
import { Suspense } from 'react';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { LoadingScreen } from '@/components/ui';
import { CourseRunList } from '@/features/training/components/course-runs/CourseRunList';

export const metadata: Metadata = {
  title: 'Course runs | Trainer | RTMS',
};

export default function TrainerCourseRunsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <Suspense fallback={<LoadingScreen />}>
          <CourseRunList />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
