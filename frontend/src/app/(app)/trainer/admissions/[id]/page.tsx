import { Metadata } from 'next';
import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { TrainerAdmissionDetailView } from '@/features/admissions/components/TrainerAdmissionDetailView';

export const metadata: Metadata = {
  title: 'Horse Admission Detail | Head Trainer',
};

type RouteParams = Promise<{ id: string }>;
type SearchParams = Promise<Record<string, string | string[] | undefined>>;

function first(value: string | string[] | undefined) {
  return Array.isArray(value) ? value[0] : value;
}

export default async function TrainerAdmissionDetailPage({
  params,
  searchParams,
}: {
  params: RouteParams;
  searchParams: SearchParams;
}) {
  const [{ id }, query] = await Promise.all([params, searchParams]);
  const admissionId = Number(id);
  const validId = Number.isSafeInteger(admissionId) && admissionId > 0;

  const urlParams = new URLSearchParams();
  for (const key of ['candidateName', 'status', 'tab', 'submittedFrom', 'submittedTo']) {
    const value = first(query[key]);
    if (value) urlParams.set(key, value);
  }
  const qs = urlParams.toString();
  const returnTo = qs ? `/trainer/admissions?${qs}` : '/trainer/admissions';

  return (
    <RoleGuard allowedRoles={['HEAD_TRAINER']}>
      <PageContainer>
        {validId ? (
          <TrainerAdmissionDetailView admissionId={admissionId} returnTo={returnTo} />
        ) : (
          <p role="alert" className="text-sm text-[var(--color-danger)]">
            Invalid admission ID.
          </p>
        )}
      </PageContainer>
    </RoleGuard>
  );
}
