import { AppShell } from '@/components/layout/AppShell';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { GroomAdmissionsTable } from '@/features/admissions/components/GroomAdmissionsTable';
import type { AdmissionStatus, GroomQueueFilters } from '@/features/admissions/types';

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

const validStatuses: AdmissionStatus[] = [
  'GROOM_REVIEW', 'WAITING_FOR_STALL', 'WAITING_FOR_ARRIVAL', 'ARRIVAL_EXPIRED', 'VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED', 'REJECTED',
];

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

export default async function GroomAdmissionsPage({ searchParams }: { searchParams: SearchParams }) {
  const query = await searchParams;
  const statusValue = first(query.status);
  const pageValue = Number(first(query.page) ?? 0);
  const initialFilters: GroomQueueFilters = {
    candidateName: first(query.candidateName) ?? '',
    status: validStatuses.includes(statusValue as AdmissionStatus) ? statusValue as AdmissionStatus : '',
    submittedFrom: first(query.submittedFrom) ?? '',
    submittedTo: first(query.submittedTo) ?? '',
    page: Number.isInteger(pageValue) && pageValue >= 0 ? pageValue : 0,
  };

  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <AppShell>
        <PageContainer><GroomAdmissionsTable initialFilters={initialFilters} /></PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
