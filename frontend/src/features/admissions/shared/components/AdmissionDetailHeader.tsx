import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { AdmissionStatusBadge } from './AdmissionStatusBadge';
import type { AdmissionStatus } from '../../types';

interface HeaderDetail {
  admissionId: number;
  status: AdmissionStatus;
  submittedAt: string;
  ownerId?: number;
  ownerName?: string | null;
  candidate: { name: string; breed?: string | null } | null;
}

function HeaderValue({ label, value }: { label: string; value: string }) {
  return <div><span className="mb-1 block text-[10px] text-[var(--color-text-muted)]">{label}</span><span className="font-medium text-[var(--color-text-primary)]">{value}</span></div>;
}

function date(value: string | null) {
  return value ? new Date(value).toLocaleDateString() : 'Not recorded';
}

export function AdmissionDetailHeader({ detail, horsePhotoUrl }: { detail: HeaderDetail; horsePhotoUrl?: string }) {
  const candidate = detail.candidate;
  if (!candidate) return null;
  
  return (
    <header className="flex shrink-0 items-center gap-4 border-b border-[var(--color-border)] px-6 py-5 bg-[var(--color-surface)]">
      <HorseAvatar name={candidate.name} image={horsePhotoUrl} size={56} rounded="md" />
      <div className="min-w-0 flex-1">
        <h1 className="mb-2 flex flex-wrap items-center gap-2 text-[20px] font-bold text-[var(--color-text-primary)]">
          <span className="truncate">{candidate.name}</span>
          <AdmissionStatusBadge status={detail.status} size="sm" />
        </h1>
        <div className="grid grid-cols-2 gap-x-5 gap-y-2 text-[12px] md:grid-cols-4">
          <HeaderValue label="ADMISSION ID" value={`#${detail.admissionId}`} />
          <HeaderValue label="OWNER" value={detail.ownerName ? `${detail.ownerName}${detail.ownerId != null ? ` (#${detail.ownerId})` : ''}` : detail.ownerId != null ? `#${detail.ownerId}` : 'You'} />
          <HeaderValue label="BREED" value={candidate.breed || 'Not provided'} />
          <HeaderValue label="SUBMITTED" value={date(detail.submittedAt)} />
        </div>
      </div>
    </header>
  );
}
