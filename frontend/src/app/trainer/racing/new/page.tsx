import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerRaceForm } from '@/features/racing/components/TrainerRaceForm';

export const metadata: Metadata = {
  title: 'Tạo đơn đề cử dự đua | Huấn luyện viên',
};

export default function TrainerNewRaceNominationPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerRaceForm />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
