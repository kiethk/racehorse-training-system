import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { PlanList } from '@/features/training/components/PlanList';

export const metadata: Metadata = {
  title: 'Kế hoạch huấn luyện | Huấn luyện viên',
};

export default function TrainerPlansPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <PlanList />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
