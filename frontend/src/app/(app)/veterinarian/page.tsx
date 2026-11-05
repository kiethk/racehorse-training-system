'use client';

import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { VetDashboardView } from '@/features/vet/components/VetDashboardView';

export default function VeterinarianPage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <VetDashboardView />
      </PageContainer>
    </RoleGuard>
  );
}
