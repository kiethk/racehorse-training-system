import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ManagerAdmissionsListView } from '@/features/admissions/components/ManagerAdmissionsListView';

export const metadata: Metadata = {
  title: 'Admissions | Manager',
};

export default function ManagerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <ManagerAdmissionsListView />
      </PageContainer>
    </RoleGuard>
  );
}
