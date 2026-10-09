import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerRaceList } from '@/features/racing/components/TrainerRaceList';

export const metadata: Metadata = {
  title: 'Race nominations | Trainer | RTMS',
};

export default function TrainerRacingPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainerRaceList />
      </PageContainer>
    </RoleGuard>
  );
}
