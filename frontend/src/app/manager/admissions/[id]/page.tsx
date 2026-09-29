import { AppShell } from '@/components/layout/AppShell';
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
  
  const query = new URLSearchParams();
  Object.entries(resolvedSearchParams).forEach(([k, v]) => {
    if (typeof v === 'string') query.set(k, v);
    else if (Array.isArray(v)) v.forEach(item => query.append(k, item));
  });
  const qs = query.toString();
  const returnTo = qs ? `/manager/admissions?${qs}` : '/manager/admissions';

  return (
    <RoleGuard allowedRoles={['CLUB_MANAGER']}>
      <AppShell>
        <PageContainer>
          {validId
            ? <ManagerAdmissionDetailView admissionId={admissionId} returnTo={returnTo} />
            : <p role="alert" className="text-sm text-[var(--color-danger)]">Invalid admission ID.</p>}
        </PageContainer>
      </AppShell>
    </RoleGuard>
  );
}
