import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { SchedulePage } from '@/features/training/components/schedule/SchedulePage';

export const metadata: Metadata = {
  title: 'Schedule | Groom | RTMS',
};

export default function GroomSchedulePage() {
  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        <SchedulePage canManage={false} />
      </PageContainer>
    </RoleGuard>
  );
}
