import { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoutineChecksWorkspace } from '@/features/routine/components/RoutineChecksWorkspace';

export const metadata: Metadata = {
  title: 'Routine & Preventive Care | RTMS',
  description: 'Manage routine checkups, vaccinations, and preventive care for horses',
};

export default function RoutineChecksPage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <RoutineChecksWorkspace />
      </PageContainer>
    </RoleGuard>
  );
}
