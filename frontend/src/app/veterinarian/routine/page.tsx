import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoutineChecksWorkspace } from '@/features/routine/components/RoutineChecksWorkspace';

export const metadata: Metadata = {
  title: 'Routine & Preventive Care | RTMS',
  description: 'Manage routine checkups, vaccinations, and preventive care for horses',
};

export default function RoutineChecksPage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <RoutineChecksWorkspace />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
