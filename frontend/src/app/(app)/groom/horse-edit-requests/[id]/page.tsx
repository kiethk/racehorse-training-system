import { PageContainer } from '@/components/layout/PageContainer';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { GroomHorseEditRequestReview } from '@/features/horses/components/GroomHorseEditRequestReview';

type RouteParams = Promise<{ id: string }>;

export default async function GroomHorseEditRequestPage({ params }: { params: RouteParams }) {
  const { id } = await params;
  const requestId = Number(id);
  const validId = Number.isSafeInteger(requestId) && requestId > 0;

  return (
    <RoleGuard allowedRoles={['GROOM']}>
      <PageContainer>
        {validId
          ? <GroomHorseEditRequestReview requestId={requestId} />
          : <p role="alert" className="text-sm text-[var(--color-danger)]">Invalid change request ID.</p>}
      </PageContainer>
    </RoleGuard>
  );
}
