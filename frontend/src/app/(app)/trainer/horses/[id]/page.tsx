import type { Metadata } from 'next';
import { notFound } from 'next/navigation';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerHorseDetailView } from '@/features/training/components/TrainerHorseDetailView';

export const metadata: Metadata = {
  title: 'Horse Profile | Trainer | RTMS',
};

export default async function TrainerHorseDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const horseId = Number(id);

  if (!Number.isInteger(horseId) || horseId <= 0) {
    notFound();
  }

  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        <TrainerHorseDetailView horseId={horseId} />
      </PageContainer>
    </RoleGuard>
  );
}
