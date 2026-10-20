import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerAdmissionsListView } from '@/features/admissions/components/TrainerAdmissionsListView';

export const metadata: Metadata = {
  title: 'Horse Admissions | Head Trainer',
};

export default function TrainerAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainerAdmissionsListView />
      </PageContainer>
    </RoleGuard>
  );
}