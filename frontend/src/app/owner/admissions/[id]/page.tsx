'use client';

import { useParams, useSearchParams } from 'next/navigation';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { EmptyState } from '@/components/ui/states';
import { OwnerAdmissionDetailView } from '@/features/admissions/components/OwnerAdmissionDetailView';

export default function OwnerAdmissionDetailPage() {
  const params = useParams();
  const search = useSearchParams();
  const id = Number(params.id);
  return (
    <RoleGuard allowedRoles={['HORSE_OWNER']}>
      <AppShell>
        <PageContainer>
          {Number.isSafeInteger(id) && id > 0
            ? <OwnerAdmissionDetailView key={id} admissionId={id} created={search.get('created') === '1'} />
            : <EmptyState title="Invalid application" description="The admission ID must be a positive integer." />}
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
