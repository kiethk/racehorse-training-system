import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerHorseListView } from '@/features/training/components/TrainerHorseListView';

export const metadata: Metadata = {
  title: 'Assigned horses | Trainer | RTMS',
};

export default function TrainerHorsesPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainerHorseListView />
      </PageContainer>
    </RoleGuard>
  );
}
