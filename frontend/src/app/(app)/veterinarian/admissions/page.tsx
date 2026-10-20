import { Suspense } from 'react';
import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { ListSkeleton } from '@/components/ui/states';
import { VetAdmissionQueue } from '@/features/admissions/components/VetAdmissionQueue';

export const metadata: Metadata = {
  title: 'Veterinary Admissions | Racehorse Training System',
  description: 'Clinical examination and admission quarantine clearance workspace for veterinarians',
};

export default function VetAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <Suspense fallback={<ListSkeleton rows={6} />}>
          <VetAdmissionQueue />
        </Suspense>
      </PageContainer>
    </RoleGuard>
  );
}
