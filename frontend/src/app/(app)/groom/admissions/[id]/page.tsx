import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { GroomAdmissionReview } from '@/features/admissions/components/GroomAdmissionReview';

type RouteParams = Promise<{ id: string }>;
type SearchParams = Promise<Record<string, string | string[] | undefined>>;

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

function buildReturnTo(query: Record<string, string | string[] | undefined>) {
  const params = new URLSearchParams();
  for (const key of ['candidateName', 'status', 'submittedFrom', 'submittedTo', 'page']) {
    const value = first(query[key]);
    if (value) params.set(key, value);
  }
  const serialized = params.toString();
  return serialized ? `/groom/admissions?${serialized}` : '/groom/admissions';
}

export default async function GroomAdmissionDetailPage({
  params,
  searchParams,
}: {
  params: RouteParams;
  searchParams: SearchParams;
}) {
  const [{ id }, query] = await Promise.all([params, searchParams]);
  const admissionId = Number(id);
  const validId = Number.isSafeInteger(admissionId) && admissionId > 0;

  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        {validId
          ? <GroomAdmissionReview admissionId={admissionId} returnTo={buildReturnTo(query)} />
          : <p role="alert" className="text-sm text-[var(--color-danger)]">Invalid admission ID.</p>}
      </PageContainer>
    </RoleGuard>
  );
}
