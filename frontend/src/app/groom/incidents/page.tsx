import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { IncidentList } from '@/features/groom/components/IncidentList';

export const metadata: Metadata = {
  title: 'Báo cáo sự cố | Groom',
};

export default function GroomIncidentsPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <AppShell>
        <PageContainer>
          <IncidentList />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
