import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { CareScheduleExamView } from '@/features/care/components/CareScheduleExamView';

export const metadata: Metadata = {
  title: 'Care Examination | Veterinarian | RTMS',
  description: 'Examination workspace for routine and preventive care visits',
};

export default async function CareScheduleExamPage({ params }: { params: Promise<{ scheduleId: string }> }) {
  const { scheduleId } = await params;

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <CareScheduleExamView scheduleId={Number(scheduleId)} />
      </PageContainer>
    </RoleGuard>
  );
}
