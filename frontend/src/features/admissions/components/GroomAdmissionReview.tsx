'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { EmptyState } from '@/components/ui/states';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse } from '../types';

function date(value: string | null) {
  return value ? new Date(value).toLocaleDateString() : 'Not recorded';
}

const documentNames: Record<string, string> = {
  HORSE_PHOTO: 'Horse photo',
  REGISTRATION_DOCUMENT: 'Registration document',
  PEDIGREE_CERTIFICATE: 'Pedigree certificate',
  VACCINATION_RECORD: 'Vaccination record',
  DEWORMING_RECORD: 'Deworming record',
  HEALTH_CERTIFICATE: 'Health certificate',
  PREVIOUS_MEDICAL_RECORD: 'Previous medical record',
  PREVIOUS_INJURY_RECORD: 'Previous injury record',
};

export function GroomAdmissionReview({ admissionId, returnTo }: { admissionId: number; returnTo: string }) {
  const [detail, setDetail] = useState<AdmissionDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState('');
  const [feedbackError, setFeedbackError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setDetail(await admissionsApi.getAdmissionDetail(admissionId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load this application.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    let active = true;
    admissionsApi.getAdmissionDetail(admissionId)
      .then((data) => { if (active) setDetail(data); })
      .catch((cause: unknown) => {
        if (active) setError(cause instanceof Error ? cause.message : 'Unable to load this application.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [admissionId]);

  const review = async (decision: 'APPROVED' | 'REJECTED') => {
    if (!feedback.trim()) {
      setFeedbackError('Feedback is required for every Groom decision.');
      return;
    }
    setSubmitting(true);
    setFeedbackError(null);
    setNotice(null);
    try {
      const updated = await admissionsApi.groomReview(admissionId, { decision, feedback: feedback.trim() });
      setDetail(updated);
      setNotice(updated.status === 'WAITING_FOR_STALL'
        ? 'Groom approved this application. Capacity is not available yet, so it is waiting for a stall.'
        : decision === 'REJECTED' ? 'Application rejected.' : 'Application approved and moved to Vet review.');
    } catch (cause) {
      setNotice(null);
      setError(cause instanceof Error ? cause.message : 'Unable to submit the Groom decision.');
    } finally {
      setSubmitting(false);
    }
  };

  const retryAllocation = async () => {
    setSubmitting(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await admissionsApi.retryQuarantineAllocation(admissionId);
      setDetail(updated);
      setNotice(updated.status === 'WAITING_FOR_STALL'
        ? 'Capacity is still unavailable. The application remains in the waiting queue.'
        : 'A quarantine stall was allocated and the application moved to Vet review.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to retry stall allocation.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="space-y-4" aria-label="Loading admission"><div className="h-24 animate-pulse rounded bg-[var(--color-surface-muted)]" /><div className="h-48 animate-pulse rounded bg-[var(--color-surface-muted)]" /></div>;
  if (error && !detail) return <EmptyState icon="alert-triangle" title="Unable to load application" description={error} action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />;
  if (!detail?.candidate) return <EmptyState title="Application not found" description="This admission record is unavailable." action={<Link className="text-sm font-medium text-[var(--color-primary)]" href={returnTo}>Back to applications</Link>} />;

  const candidate = detail.candidate;
  const horsePhoto = detail.documents.find((doc) => doc.documentType === 'HORSE_PHOTO');
  const canReview = detail.status === 'GROOM_REVIEW';
  const waiting = detail.status === 'WAITING_FOR_STALL';
  const capacity = detail.capacity;
  const isReady = capacity?.admissionCapacityAvailable ?? false;
  const blockingText = capacity?.blockingReason === 'NO_QUARANTINE_STALL'
    ? 'No quarantine stall is available.'
    : capacity?.blockingReason === 'REGULAR_RESERVE_INSUFFICIENT'
      ? 'Regular-stall reserve is below the required quarantine turnover reserve.'
      : 'The current capacity snapshot could not be loaded.';

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Link href={returnTo} className="inline-flex items-center gap-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"><Icon name="arrow-left" size={15} />Back to applications</Link>
        <span className="rounded-full bg-[var(--color-surface-muted)] px-3 py-1 text-xs font-medium text-[var(--color-text-secondary)]">{detail.status.replaceAll('_', ' ')}</span>
      </div>
      <header className="flex items-center gap-4 border-b border-[var(--color-border)] pb-5">
        <HorseAvatar name={candidate.name} image={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} size={64} rounded="md" />
        <div className="min-w-0">
          <h1 className="truncate text-xl font-semibold text-[var(--color-text-primary)]">{candidate.name}</h1>
          <p className="mt-1 text-sm text-[var(--color-text-secondary)]">{candidate.breed || 'Breed not provided'} · Submitted {date(detail.submittedAt)}</p>
        </div>
      </header>

      {notice && <div role="status" className="border-l-2 border-[var(--color-success)] bg-[var(--color-success-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{notice}</div>}
      {error && detail && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

      <section className="grid gap-5 lg:grid-cols-[minmax(0,1.2fr)_minmax(320px,0.8fr)]">
        <div className="space-y-5">
          <section className="border-y border-[var(--color-border)] py-4">
            <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Basic information</h2>
            <dl className="mt-4 grid gap-x-6 gap-y-4 sm:grid-cols-2">
              <Info label="Horse name" value={candidate.name} />
              <Info label="Breed" value={candidate.breed} />
              <Info label="Date of birth" value={date(candidate.dateOfBirth)} />
              <Info label="Registration number" value={candidate.registrationNumber} />
              <Info label="Registry" value={candidate.registryName} />
              <Info label="Owner" value={detail.ownerName ? `${detail.ownerName} (ID ${detail.ownerId})` : `ID ${detail.ownerId}`} />
              <Info label="Sire" value={candidate.sireName} />
              <Info label="Dam" value={candidate.damName} />
            </dl>
          </section>

          <section className="border-y border-[var(--color-border)] py-4">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Submitted documents</h2>
              <span className="text-xs text-[var(--color-text-muted)]">{detail.documents.length} files</span>
            </div>
            {detail.documents.length ? (
              <ul className="mt-3 divide-y divide-[var(--color-border)]">
                {detail.documents.map((document) => (
                  <li key={document.id} className="flex min-w-0 items-center gap-3 py-3">
                    <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded bg-[var(--color-surface-muted)] text-[var(--color-text-secondary)]"><Icon name={document.documentType === 'HORSE_PHOTO' ? 'image' : 'file-text'} size={16} /></span>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">{document.originalFileName || documentNames[document.documentType] || document.documentType}</p>
                      <p className="mt-0.5 truncate text-xs text-[var(--color-text-secondary)]">{documentNames[document.documentType] || document.documentType} · Recorded {date(document.recordDate)} · Uploaded {date(document.uploadedAt)}</p>
                    </div>
                    <a href={admissionsApi.assetUrl(document.fileUrl)} target="_blank" rel="noreferrer" className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)]" aria-label={`Open ${document.originalFileName || documentNames[document.documentType] || 'document'}`} title="Open document"><Icon name="external-link" size={15} /></a>
                  </li>
                ))}
              </ul>
            ) : <p className="py-4 text-sm text-[var(--color-text-muted)]">No documents were submitted.</p>}
          </section>
        </div>

        <aside className="space-y-5">
          <section className="border-y border-[var(--color-border)] py-4">
            <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Quarantine capacity</h2>
            <dl className="mt-3 space-y-2 text-sm">
              <CapacityRow label="Available quarantine stalls" value={capacity?.availableQuarantineStalls} />
              <CapacityRow label="Available regular stalls" value={capacity?.availableRegularStalls} />
              <CapacityRow label="Occupied quarantine stalls" value={capacity?.occupiedQuarantineStalls} />
            </dl>
            <p className={`mt-3 text-sm ${isReady ? 'text-[var(--color-success)]' : 'text-[var(--color-warning)]'}`}>
              {isReady ? 'Capacity is sufficient for admission.' : blockingText}
            </p>
            {!isReady && capacity && <p className="mt-1 text-xs text-[var(--color-text-secondary)]">Regular reserve required: {capacity.occupiedQuarantineStalls + 1} available regular stall(s).</p>}
          </section>

          {canReview && (
            <section className="space-y-3 border-y border-[var(--color-border)] py-4">
              <label htmlFor="groom-feedback" className="block text-sm font-semibold text-[var(--color-text-primary)]">Groom feedback <span className="text-[var(--color-danger)]">*</span></label>
              <textarea id="groom-feedback" value={feedback} onChange={(event) => { setFeedback(event.target.value); setFeedbackError(null); }} rows={5} maxLength={2000} required aria-invalid={Boolean(feedbackError)} className="w-full resize-y rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
              <div className="flex justify-between text-xs">
                {feedbackError ? <span role="alert" className="text-[var(--color-danger)]">{feedbackError}</span> : <span className="text-[var(--color-text-muted)]">Required for approval and rejection</span>}
                <span className="text-[var(--color-text-muted)]">{feedback.length}/2000</span>
              </div>
              <div className="flex flex-wrap gap-2 pt-1">
                <Button type="button" size="sm" loading={submitting} disabled={!feedback.trim() || !isReady} onClick={() => void review('APPROVED')} className="bg-[var(--color-success)] text-white hover:opacity-90">Accept</Button>
                <Button type="button" size="sm" loading={submitting} disabled={!feedback.trim() || isReady} onClick={() => void review('APPROVED')} className="bg-[var(--color-warning)] text-white hover:opacity-90">Approve &amp; wait</Button>
                <Button type="button" size="sm" variant="destructive" loading={submitting} disabled={!feedback.trim()} onClick={() => void review('REJECTED')}>Reject</Button>
              </div>
            </section>
          )}

          {waiting && (
            <section className="space-y-3 border-y border-[var(--color-border)] py-4">
              <div>
                <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Waiting for stall</h2>
                <p className="mt-1 text-sm text-[var(--color-text-secondary)]">Groom feedback: {detail.groomFeedback || 'Not recorded'}</p>
              </div>
              <Button type="button" size="sm" loading={submitting} onClick={() => void retryAllocation()} className="bg-[var(--color-warning)] text-white hover:opacity-90">Retry allocation</Button>
            </section>
          )}

          {!canReview && !waiting && <p className="border-y border-[var(--color-border)] py-4 text-sm text-[var(--color-text-secondary)]">This application is read-only at its current stage.</p>}
        </aside>
      </section>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string | null | undefined }) {
  return <div className="min-w-0"><dt className="text-xs text-[var(--color-text-muted)]">{label}</dt><dd className="mt-1 break-words text-sm text-[var(--color-text-primary)]">{value || 'Not provided'}</dd></div>;
}

function CapacityRow({ label, value }: { label: string; value: number | undefined }) {
  return <div className="flex justify-between gap-3"><dt className="text-[var(--color-text-secondary)]">{label}</dt><dd className="font-medium tabular-nums text-[var(--color-text-primary)]">{value ?? '—'}</dd></div>;
}
