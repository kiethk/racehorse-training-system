'use client';

import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { Pill } from '@/components/ui/StatusBadge';
import type { PendingVetOfferResponse } from '../types';

interface VetOfferModalProps {
  offer: PendingVetOfferResponse | null;
  countdown: string;
  actioning: boolean;
  error?: string;
  onAccept: (offer: PendingVetOfferResponse) => void;
  onDeny: (offer: PendingVetOfferResponse) => void;
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'full',
    timeStyle: 'short',
  }).format(date);
}

function formatCareType(value: string): string {
  return value.replaceAll('_', ' ').toLowerCase().replace(/^./, (letter) => letter.toUpperCase());
}

export function VetOfferModal({
  offer,
  countdown,
  actioning,
  error,
  onAccept,
  onDeny,
}: VetOfferModalProps) {
  if (!offer) return null;

  const expired = countdown === 'Expired';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/45 backdrop-blur-sm" aria-hidden="true" />
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="vet-offer-title"
        aria-describedby="vet-offer-description"
        className="relative w-full max-w-lg overflow-hidden rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-2xl shadow-black/20"
      >
        <div className="border-b border-[var(--color-border)] bg-[var(--color-warning-soft)] px-6 py-5">
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-start gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-[var(--color-warning)] text-white">
                <Icon name="bell" size={20} />
              </span>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-warning)]">
                  New veterinary offer
                </p>
                <h2 id="vet-offer-title" className="mt-0.5 text-[18px] font-bold text-[var(--color-text-primary)]">
                  Examination for {offer.horseName}
                </h2>
                <p id="vet-offer-description" className="mt-1 text-[12px] text-[var(--color-text-secondary)]">
                  Review the proposed schedule and respond before the offer expires.
                </p>
              </div>
            </div>
            <Pill tone={offer.careType === 'URGENT' ? 'danger' : 'info'}>
              {formatCareType(offer.careType)}
            </Pill>
          </div>
        </div>

        <div className="space-y-4 p-6">
          {error && (
            <div role="alert" className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
              {error}
            </div>
          )}

          <dl className="grid grid-cols-2 gap-x-5 gap-y-4 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-4 text-[12px]">
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Admission</dt>
              <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">
                {offer.admissionId ? `#${offer.admissionId}` : 'General care'}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Schedule</dt>
              <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">#{offer.careScheduleId}</dd>
            </div>
            <div className="col-span-2">
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Proposed time</dt>
              <dd className="mt-1 flex items-center gap-1.5 font-semibold text-[var(--color-text-primary)]">
                <Icon name="calendar" size={14} className="text-[var(--color-info)]" />
                {formatDateTime(offer.proposedScheduledAt)}
              </dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Duration</dt>
              <dd className="mt-1 font-semibold text-[var(--color-text-primary)]">{offer.durationMinutes || 30} minutes</dd>
            </div>
            <div>
              <dt className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Offer expires in</dt>
              <dd className={`mt-1 font-metric font-bold ${expired ? 'text-[var(--color-danger)]' : 'text-[var(--color-warning)]'}`}>
                {countdown}
              </dd>
            </div>
          </dl>

          {offer.description && (
            <div className="rounded-[var(--radius-md)] border border-[var(--color-border)] p-3">
              <p className="text-[10px] font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Clinical request</p>
              <p className="mt-1 text-[12px] leading-relaxed text-[var(--color-text-secondary)]">{offer.description}</p>
            </div>
          )}

          {expired && (
            <div role="alert" className="rounded-[var(--radius-md)] bg-[var(--color-danger-soft)] p-3 text-[12px] text-[var(--color-danger)]">
              This offer has expired. The queue will refresh and assign another veterinarian.
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-6 py-4">
          <Button
            variant="destructive"
            disabled={actioning || expired}
            loading={actioning}
            onClick={() => onDeny(offer)}
          >
            Deny
          </Button>
          <Button
            variant="primary"
            icon="check"
            disabled={actioning || expired}
            loading={actioning}
            onClick={() => onAccept(offer)}
          >
            Accept
          </Button>
        </div>
      </div>
    </div>
  );
}
