import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerHorseListView } from '@/features/training/components/TrainerHorseListView';

export const metadata: Metadata = {
  title: 'Danh sách chiến mã | Huấn luyện viên',
};

export default function TrainerHorsesPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerHorseListView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
