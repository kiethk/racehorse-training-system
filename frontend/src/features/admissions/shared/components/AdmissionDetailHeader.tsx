import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { formatDate } from '@/lib/display';
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
  return (
    <div className="min-w-0">
      <span className="mb-0.5 block text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]">{label}</span>
      <span className="break-words text-sm font-medium text-[var(--color-text-primary)]">{value}</span>
    </div>
  );
}

function date(value: string | null) {
  return value ? formatDate(value) : 'Not recorded';
}

export function AdmissionDetailHeader({ detail, horsePhotoUrl, simplifiedStatus = false }: {
  detail: HeaderDetail;
  horsePhotoUrl?: string;
  /** Passed down to the badge: show only In Progress / Approved / Rejected. */
  simplifiedStatus?: boolean;
}) {
  const candidate = detail.candidate;
  if (!candidate) return null;

  return (
    <header className="flex min-w-0 shrink-0 items-start gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:items-center sm:gap-4 sm:px-6 sm:py-5">
      <HorseAvatar name={candidate.name} image={horsePhotoUrl} size={56} rounded="md" />
      <div className="min-w-0 flex-1">
        <h1 className="mb-2.5 flex flex-wrap items-center gap-2.5 text-xl font-semibold tracking-tight text-[var(--color-text-primary)]">
          <span className="truncate">{candidate.name}</span>
          <AdmissionStatusBadge status={detail.status} simplified={simplifiedStatus} />
        </h1>
        <div className="grid min-w-0 grid-cols-1 gap-x-5 gap-y-2 min-[480px]:grid-cols-2 lg:grid-cols-4">
          <HeaderValue label="Admission ID" value={`#${detail.admissionId}`} />
          <HeaderValue label="Owner" value={detail.ownerName ? `${detail.ownerName}${detail.ownerId != null ? ` (#${detail.ownerId})` : ''}` : detail.ownerId != null ? `#${detail.ownerId}` : 'You'} />
          <HeaderValue label="Breed" value={candidate.breed || 'Not provided'} />
          <HeaderValue label="Submitted" value={date(detail.submittedAt)} />
        </div>
      </div>
    </header>
  );
}
