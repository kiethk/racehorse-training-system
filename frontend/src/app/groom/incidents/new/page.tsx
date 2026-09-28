import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { IncidentForm } from '@/features/groom/components/IncidentForm';

export const metadata: Metadata = {
  title: 'Báo cáo sự cố mới | Groom',
};

export default function NewIncidentPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <AppShell>
        <PageContainer>
          <IncidentForm />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
