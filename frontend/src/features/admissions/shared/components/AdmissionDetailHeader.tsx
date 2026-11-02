'use client';

import { useState, type ReactNode } from 'react';
import Link from 'next/link';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { ImageLightbox } from '@/components/ui/ImageLightbox';
import { useProtectedFileUrl } from '@/lib/protectedFile';
import { formatDate } from '@/lib/display';
import { formatHorseSex } from '@/features/horses/lib/sex';
import type { HorseSex } from '@/types/horse';
import { AdmissionStatusBadge } from './AdmissionStatusBadge';
import type { AdmissionStatus } from '../../types';

interface HeaderDetail {
  admissionId: number;
  status: AdmissionStatus;
  submittedAt: string;
  ownerId?: number;
  ownerName?: string | null;
  candidate: { name: string; breed?: string | null; sex?: HorseSex | null } | null;
}

function HeaderValue({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="min-w-0">
      <span className="mb-0.5 block text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]">{label}</span>
      <div className="break-words text-sm font-medium text-[var(--color-text-primary)]">{value}</div>
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
  const [photoOpen, setPhotoOpen] = useState(false);
  const candidate = detail.candidate;

  const effectivePhoto =
    horsePhotoUrl ||
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (detail as any).documents?.find((doc: any) => doc.documentType === 'HORSE_PHOTO')?.fileUrl ||
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    (candidate as any)?.imageUrl ||
    null;
  const { url: protectedPhotoUrl } = useProtectedFileUrl(effectivePhoto);

  if (!candidate) return null;

  return (
    <>
      <header className="flex min-w-0 shrink-0 items-start gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:items-center sm:gap-4 sm:px-6 sm:py-5">
        {protectedPhotoUrl ? (
          <button
            type="button"
            onClick={() => setPhotoOpen(true)}
            className="group relative shrink-0 cursor-pointer overflow-hidden rounded-[var(--radius-md)] focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
            title={`Preview full photo of ${candidate.name}`}
          >
            <HorseAvatar name={candidate.name} image={protectedPhotoUrl} size={56} rounded="md" />
            <span className="absolute inset-0 flex items-center justify-center gap-1 rounded-[var(--radius-md)] bg-black/40 text-[10px] font-semibold text-white opacity-0 transition-opacity group-hover:opacity-100 backdrop-blur-[1px]">
              <Icon name="maximize" size={13} />
            </span>
          </button>
        ) : (
          <HorseAvatar name={candidate.name} size={56} rounded="md" />
        )}

        <div className="min-w-0 flex-1">
          <h1 className="mb-2.5 flex flex-wrap items-center gap-2.5 text-xl font-semibold tracking-tight text-[var(--color-text-primary)]">
            <span className="truncate">{candidate.name}</span>
            <AdmissionStatusBadge status={detail.status} simplified={simplifiedStatus} />
          </h1>
          <div className="grid min-w-0 grid-cols-1 gap-x-5 gap-y-2 min-[480px]:grid-cols-2 lg:grid-cols-5">
            <HeaderValue label="Admission ID" value={`#${detail.admissionId}`} />
            <HeaderValue
              label="Owner"
              value={
                detail.ownerId != null ? (
                  <Link
                    href={`/manager/accounts?userId=${detail.ownerId}`}
                    className="group inline-flex items-center gap-1 font-semibold text-[var(--color-primary)] hover:underline truncate max-w-full"
                    title="View owner account details"
                  >
                    <span className="truncate">{detail.ownerName ? `${detail.ownerName} (#${detail.ownerId})` : `#${detail.ownerId}`}</span>
                    <Icon name="external-link" size={11} className="shrink-0 opacity-70 group-hover:opacity-100 transition-opacity" />
                  </Link>
                ) : (
                  detail.ownerName || 'You'
                )
              }
            />
            <HeaderValue label="Breed" value={candidate.breed || 'Not provided'} />
            <HeaderValue label="Sex" value={formatHorseSex(candidate.sex)} />
            <HeaderValue label="Submitted" value={date(detail.submittedAt)} />
          </div>
        </div>
      </header>

      {photoOpen && protectedPhotoUrl && (
        <ImageLightbox
          images={[{ src: protectedPhotoUrl, alt: `${candidate.name}'s admission photo` }]}
          index={0}
          onIndexChange={() => undefined}
          onClose={() => setPhotoOpen(false)}
        />
      )}
    </>
  );
}
