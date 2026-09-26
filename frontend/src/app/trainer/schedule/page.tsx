import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { LotTimeline } from '@/features/training/components/LotTimeline';

export const metadata: Metadata = {
  title: 'Lịch Lot huấn luyện | Huấn luyện viên',
};

export default function TrainerSchedulePage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <LotTimeline />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
