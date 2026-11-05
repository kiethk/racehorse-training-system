import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { GroomHorseEditRequestsView } from '@/features/horses/components/GroomHorseEditRequestsView';

export default function GroomHorseEditRequestsPage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer><GroomHorseEditRequestsView /></PageContainer>
    </RoleGuard>
  );
}
