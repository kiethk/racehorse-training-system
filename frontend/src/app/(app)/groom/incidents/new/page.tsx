import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { IncidentForm } from '@/features/groom/components/IncidentForm';

export const metadata: Metadata = {
  title: 'New Incident Report | Groom',
};

export default function NewIncidentPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        <IncidentForm />
      </PageContainer>
    </RoleGuard>
  );
}
