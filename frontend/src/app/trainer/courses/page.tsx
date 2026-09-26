import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainingCoursesView } from '@/features/training/components/TrainingCoursesView';

export const metadata: Metadata = {
  title: 'Khóa học & Bài tập | Huấn luyện viên',
};

export default function TrainerCoursesPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainingCoursesView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
