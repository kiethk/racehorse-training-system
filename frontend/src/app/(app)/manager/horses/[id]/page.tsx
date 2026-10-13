import type { Metadata } from 'next';
import { notFound } from 'next/navigation';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { HorseProfileView } from '@/features/horses/shared/components/HorseProfileView';

export const metadata: Metadata = {
  title: 'Horse Profile | Manager | RTMS',
};

export default async function ManagerHorseDetailPage({
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
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        <HorseProfileView
          horseId={horseId}
          returnTo="/manager/horses"
          backLabel="Back to horses"
        />
      </PageContainer>
    </RoleGuard>
  );
}
