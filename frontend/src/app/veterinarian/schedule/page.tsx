import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { VetScheduleTimeline } from '@/features/schedule/components/VetScheduleTimeline';

export const metadata: Metadata = {
  title: 'Veterinary Schedule & Timeline | RTMS',
  description: 'Timeline and schedule management for examinations, urgent cases, and routine care',
};

export default function VetSchedulePage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <VetScheduleTimeline />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
