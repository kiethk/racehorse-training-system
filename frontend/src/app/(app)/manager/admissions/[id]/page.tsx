import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { ManagerAdmissionDetailView } from '@/features/admissions/components/ManagerAdmissionDetailView';

type RouteParams = Promise<{ id: string }>;
type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default async function ManagerAdmissionDetailPage({
  params,
  searchParams,
}: {
  params: RouteParams;
  searchParams: SearchParams;
}) {
  const [{ id }, resolvedSearchParams] = await Promise.all([params, searchParams]);
  const admissionId = Number(id);
  const validId = Number.isSafeInteger(admissionId) && admissionId > 0;
  
  const FILTER_KEYS = ['candidateName', 'status', 'submittedFrom', 'submittedTo'] as const;
  const VALID_STATUSES = ['ALL', 'GROOM_REVIEW', 'WAITING_FOR_STALL', 'WAITING_FOR_ARRIVAL', 'ARRIVAL_EXPIRED', 'VET_REVIEW', 'TRAINER_REVIEW', 'MANAGER_REVIEW', 'APPROVED', 'REJECTED'];
  
  const query = new URLSearchParams();
  FILTER_KEYS.forEach((key) => {
    const v = resolvedSearchParams[key];
    if (typeof v !== 'string' || !v) return;
    // Validate status against allowed values
    if (key === 'status' && !VALID_STATUSES.includes(v)) return;
    query.set(key, v);
  });
  const qs = query.toString();
  const returnTo = qs ? `/manager/admissions?${qs}` : '/manager/admissions';

  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <PageContainer>
        {validId
          ? <ManagerAdmissionDetailView admissionId={admissionId} returnTo={returnTo} />
          : <p role="alert" className="text-sm text-[var(--color-danger)]">Invalid admission ID.</p>}
      </PageContainer>
    </RoleGuard>
  );
}
