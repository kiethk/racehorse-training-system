import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { HorseFitnessTrendView } from '@/features/training/components/HorseFitnessTrendView';

export const metadata: Metadata = {
  title: 'Horse fitness and progress | Trainer | RTMS',
};

export default async function HorseFitnessDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const resolvedParams = await params;
  const horseId = Number(resolvedParams.id);

  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          <HorseFitnessTrendView horseId={horseId} />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
