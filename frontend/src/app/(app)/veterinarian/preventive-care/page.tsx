import { Suspense } from 'react';
import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { PreventiveCareHub } from '@/features/care/components/preventive/PreventiveCareHub';
import { ListSkeleton } from '@/components/ui';

export const metadata: Metadata = {
  title: 'Preventive Care | Veterinarian | RTMS',
  description: 'Vaccinations, deworming and routine visits for every horse in the stable',
};

export default function PreventiveCarePage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN', 'CLUB_MANAGER']}>
      <PageContainer>
        <Suspense fallback={<ListSkeleton rows={4} />}>
          <PreventiveCareHub />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
