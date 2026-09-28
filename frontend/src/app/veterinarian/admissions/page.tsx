import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { VetAdmissionQueue } from '@/features/admissions/components/VetAdmissionQueue';

export const metadata: Metadata = {
  title: 'Veterinary Admissions | Racehorse Training System',
  description: 'Clinical examination and admission quarantine clearance workspace for veterinarians',
};

export default function VetAdmissionsPage() {
  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <AppShell>
        <PageContainer>
          <VetAdmissionQueue />
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
