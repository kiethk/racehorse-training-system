import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { SchedulePage } from '@/features/training/components/SchedulePage';

export const metadata: Metadata = {
  title: 'Schedule | Trainer | RTMS',
};

export default function TrainerSchedulePage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <SchedulePage canManage />
      </PageContainer>
    </RoleGuard>
  );
}
