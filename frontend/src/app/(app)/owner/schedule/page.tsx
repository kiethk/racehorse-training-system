import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { SchedulePage } from '@/features/training/components/schedule/SchedulePage';

export const metadata: Metadata = {
  title: 'Schedule | Owner | RTMS',
};

export default function OwnerSchedulePage() {
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <PageContainer>
        <SchedulePage canManage={false} showTrainers />
      </PageContainer>
    </RoleGuard>
  );
}
