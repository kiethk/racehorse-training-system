import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TodayChecklist } from '@/features/groom/components/TodayChecklist';

export const metadata: Metadata = {
  title: "Today's Tasks | Groom",
};

export default function GroomTasksPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        <TodayChecklist />
      </PageContainer>
    </RoleGuard>
  );
}
