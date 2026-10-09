import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { OwnerAdmissionsListView } from '@/features/admissions/components/OwnerAdmissionsListView';

export default function OwnerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <PageContainer>
        <OwnerAdmissionsListView />
      </PageContainer>
    </RoleGuard>
  );
}
