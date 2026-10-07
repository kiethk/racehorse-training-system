import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { UrgentCaseView } from '@/features/admissions/components/UrgentCaseView';

export const metadata: Metadata = {
  title: 'Urgent Veterinary Case | Racehorse Training System',
  description: 'Assigned urgent veterinary case workspace',
};

export default async function UrgentVeterinaryCasePage({ params }: { params: Promise<{ scheduleId: string }> }) {
  const { scheduleId } = await params;
  const parsedScheduleId = Number(scheduleId);

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <UrgentCaseView scheduleId={parsedScheduleId} />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
