import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerHorseListView } from '@/features/training/components/TrainerHorseListView';

export const metadata: Metadata = {
  title: 'Assigned Horses | RTMS',
};

export default function TrainerHorsesPage() {
  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <TrainerHorseListView />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
