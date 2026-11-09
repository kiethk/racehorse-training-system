import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerIncidentList } from '@/features/training/components/TrainerIncidentList';

export const metadata: Metadata = {
  title: 'Incidents | Trainer | RTMS',
};

export default function TrainerIncidentsPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainerIncidentList />
      </PageContainer>
    </RoleGuard>
  );
}
