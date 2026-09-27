import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TodayChecklist } from '@/features/groom/components/TodayChecklist';

export const metadata: Metadata = {
  title: 'Công việc hôm nay | Groom',
};

export default function GroomTasksPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <AppShell>
        <PageContainer>
          <TodayChecklist />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
