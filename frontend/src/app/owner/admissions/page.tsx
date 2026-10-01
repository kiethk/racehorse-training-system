import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { OwnerAdmissionsListView } from '@/features/admissions/components/OwnerAdmissionsListView';

export default function OwnerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <AppShell>
        <PageContainer>
          <OwnerAdmissionsListView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
