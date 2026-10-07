import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { TrainerRaceDetail } from '@/features/racing/components/TrainerRaceDetail';

export const metadata: Metadata = {
  title: 'Race nomination details | Trainer | RTMS',
};

type RouteParams = Promise<{ id: string }>;

export default async function TrainerRaceDetailPage({
  params,
}: {
  params: RouteParams;
}) {
  const { id } = await params;
  const raceId = Number(id);
  const validId = Number.isSafeInteger(raceId) && raceId > 0;

  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <AppShell>
        <PageContainer>
          {validId ? (
            <TrainerRaceDetail id={raceId} />
          ) : (
            <p role="alert" className="text-sm text-[var(--color-danger)]">
              This race nomination ID is invalid.
            </p>
          )}
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
