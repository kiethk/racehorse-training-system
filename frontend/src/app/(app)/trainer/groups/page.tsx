import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { GroupsView } from '@/features/training/components/GroupsView';

export const metadata: Metadata = {
  title: 'Groups | Trainer | RTMS',
};

export default function TrainerGroupsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <GroupsView />
      </PageContainer>
    </RoleGuard>
  );
}
