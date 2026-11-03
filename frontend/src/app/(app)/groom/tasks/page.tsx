import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { TodayChecklist } from '@/features/groom/components/TodayChecklist';

export const metadata: Metadata = {
  title: "Today's Tasks | Groom",
};

export default async function GroomTasksPage({
  searchParams,
}: {
  searchParams: Promise<{ horseId?: string }>;
}) {
  // Set when the checklist is opened from a stall on the Stable screen.
  const horseId = Number((await searchParams).horseId);

  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        <TodayChecklist horseId={Number.isInteger(horseId) && horseId > 0 ? horseId : undefined} />
      </PageContainer>
    </RoleGuard>
  );
}
