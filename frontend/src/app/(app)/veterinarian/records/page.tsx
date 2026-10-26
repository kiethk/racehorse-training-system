import type { Metadata } from 'next';
import { RoleGuard } from '@/components/auth/RoleGuard';
import { PageContainer } from '@/components/layout/PageContainer';
import { RecordDetailView } from '@/features/care/components/RecordDetailView';

export const metadata: Metadata = {
  title: 'Clinical Record | Veterinarian | RTMS',
  description: 'Full clinical examination record with 3D injury map, treatment plans and vitals trend',
};

type SearchParams = Record<string, string | string[] | undefined>;

function toNumber(value: string | string[] | undefined): number | null {
  if (typeof value !== 'string' || value === '') return null;
  const n = Number(value);
  return Number.isNaN(n) ? null : n;
}

export default async function VetClinicalRecordPage({
  searchParams,
}: {
  searchParams: Promise<SearchParams>;
}) {
  const sp = await searchParams;
  const scheduleId = toNumber(sp.scheduleId);
  const recordId = toNumber(sp.recordId);
  const horseId = toNumber(sp.horseId);
  const horseName = typeof sp.horseName === 'string' && sp.horseName ? sp.horseName : undefined;
  const fromSchedule = sp.back === 'schedule';

  const backHref =
    recordId != null && horseId != null
      ? `/veterinarian/health?horseId=${horseId}`
      : horseId != null
        ? '/veterinarian/health'
        : fromSchedule
          ? '/veterinarian/schedule'
          : scheduleId != null
            ? '/veterinarian/routine'
            : '/veterinarian';

  return (
    <RoleGuard allowedRoles={['VETERINARIAN']}>
      <PageContainer>
        <RecordDetailView
          scheduleId={scheduleId}
          recordId={recordId}
          horseId={horseId}
          horseName={horseName}
          backHref={backHref}
        />
      </PageContainer>
    </RoleGuard>
  );
}
