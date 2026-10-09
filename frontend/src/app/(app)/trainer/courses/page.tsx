import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainingCoursesView } from '@/features/training/components/TrainingCoursesView';

export const metadata: Metadata = {
  title: 'Courses and subjects | Trainer | RTMS',
};

export default function TrainerCoursesPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainingCoursesView />
      </PageContainer>
    </RoleGuard>
  );
}
