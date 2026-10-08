import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerRaceList } from '@/features/racing/components/TrainerRaceList';

export const metadata: Metadata = {
  title: 'Đề cử dự đua | Huấn luyện viên',
};

export default function TrainerRacingPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerRaceList />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
