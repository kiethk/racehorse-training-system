import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { VetScheduleTimeline } from '@/features/care/shared/components/VetScheduleTimeline';

export const metadata: Metadata = {
  title: 'Veterinary Schedule & Timeline | RTMS',
  description: 'Timeline and schedule management for examinations, urgent cases, and routine care',
};

export default function VetSchedulePage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <VetScheduleTimeline />
      </PageContainer>
    </RoleGuard>
  );
}
