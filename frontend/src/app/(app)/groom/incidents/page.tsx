import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { IncidentList } from '@/features/groom/components/IncidentList';

export const metadata: Metadata = {
  title: 'Incident Reports | Groom',
};

export default function GroomIncidentsPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        <IncidentList />
      </PageContainer>
    </RoleGuard>
  );
}
