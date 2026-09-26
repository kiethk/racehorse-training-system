import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerQueueList } from '@/features/admissions/components/TrainerQueueList';

export const metadata: Metadata = {
  title: 'Tiếp nhận chiến mã | Huấn luyện viên',
};

export default function TrainerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerQueueList />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}