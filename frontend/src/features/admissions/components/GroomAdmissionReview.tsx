'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { HorseAvatar } from '@/components/ui/HorseAvatar';
import { Icon } from '@/components/ui/Icon';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse, AdmissionStatus } from '../types';

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

interface Props {
  admissionId: number;
  returnTo: string;
  embedded?: boolean;
  onUpdated?: () => void;
}

export function GroomAdmissionReview({ admissionId, returnTo, embedded = false, onUpdated }: Props) {
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
      onUpdated?.();
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
      onUpdated?.();
      setNotice(updated.status === 'WAITING_FOR_STALL'
        ? 'Capacity is still unavailable. The application remains in the waiting queue.'
        : 'A quarantine stall was allocated and the application moved to Vet review.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to retry stall allocation.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="p-6"><ListSkeleton rows={8} /></div>;
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
    <div className={embedded ? 'flex h-[750px] flex-col overflow-y-auto scroll-slim' : 'space-y-5'}>
      {!embedded && (
        <div className="flex flex-wrap items-center justify-between gap-3">
          <Link href={returnTo} className="inline-flex items-center gap-2 text-sm font-medium text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]"><Icon name="arrow-left" size={15} />Back to applications</Link>
          <Pill tone={statusTone(detail.status)}>{prettyStatus(detail.status)}</Pill>
        </div>
      )}

      <header className="flex shrink-0 items-center gap-4 border-b border-[var(--color-border)] px-6 py-5">
        <HorseAvatar name={candidate.name} image={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} size={56} rounded="md" />
        <div className="min-w-0 flex-1">
          <h1 className="mb-2 flex flex-wrap items-center gap-2 text-[20px] font-bold text-[var(--color-text-primary)]">
            <span className="truncate">{candidate.name}</span>
            <Pill tone={statusTone(detail.status)} size="sm">{prettyStatus(detail.status)}</Pill>
          </h1>
          <div className="grid grid-cols-2 gap-x-5 gap-y-2 text-[12px] md:grid-cols-4">
            <HeaderValue label="OWNER" value={detail.ownerName ? `${detail.ownerName} (#${detail.ownerId})` : `#${detail.ownerId}`} />
            <HeaderValue label="BREED" value={candidate.breed || 'Not provided'} />
            <HeaderValue label="DATE OF BIRTH" value={date(candidate.dateOfBirth)} />
            <HeaderValue label="SUBMITTED" value={date(detail.submittedAt)} />
          </div>
        </div>
      </header>

      <div className={embedded ? 'flex-1 space-y-6 bg-[var(--color-surface-subtle)] p-6' : 'space-y-5'}>
        {notice && <div role="status" className="border-l-2 border-[var(--color-success)] bg-[var(--color-success-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{notice}</div>}
        {error && detail && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

        <Panel padded className="bg-[var(--color-surface)]">
          <SectionTitle>Review pipeline</SectionTitle>
          <div className="mt-4 flex flex-wrap gap-2 text-[12px]">
            <PipelineStep label="Groom review" isDone={!!detail.groomReviewedAt} isActive={detail.status === 'GROOM_REVIEW'} />
            <PipelineStep label="Stall assignment" isDone={!!detail.quarantineStallCode} isActive={detail.status === 'WAITING_FOR_STALL'} />
            <PipelineStep label="Veterinarian review" isDone={!!detail.vetReviewedAt} isActive={detail.status === 'VET_REVIEW'} />
            <PipelineStep label="Head Trainer review" isDone={!!detail.trainerReviewedAt} isActive={detail.status === 'TRAINER_REVIEW'} />
            <PipelineStep label="Manager final review" isDone={!!detail.managerReviewedAt} isActive={detail.status === 'MANAGER_REVIEW'} />
          </div>
        </Panel>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Panel padded className="bg-[var(--color-surface)]">
            <SectionTitle>Pedigree &amp; registration</SectionTitle>
            <div className="mt-4 space-y-3 text-[12px]">
              <InfoRow label="Registry name" value={candidate.registryName} />
              <InfoRow label="Registration no." value={candidate.registrationNumber} />
              <InfoRow label="Date of birth" value={date(candidate.dateOfBirth)} />
              <div className="space-y-3 border-t border-[var(--color-border)] pt-3">
                <InfoRow label="Sire" value={candidate.sireName} />
                <InfoRow label="Dam" value={candidate.damName} />
                {candidate.pedigreeNotes && <p className="text-[var(--color-text-secondary)] italic">&quot;{candidate.pedigreeNotes}&quot;</p>}
              </div>
            </div>
          </Panel>

          <Panel padded className="bg-[var(--color-surface)]">
            <SectionTitle>Admission capacity</SectionTitle>
            <div className="mt-4 space-y-3 text-[12px]">
              <InfoRow label="Available quarantine stalls" value={String(capacity?.availableQuarantineStalls ?? '—')} />
              <InfoRow label="Available regular stalls" value={String(capacity?.availableRegularStalls ?? '—')} />
              <InfoRow label="Occupied quarantine stalls" value={String(capacity?.occupiedQuarantineStalls ?? '—')} />
              <div className={`border-t border-[var(--color-border)] pt-3 font-medium ${isReady ? 'text-[var(--color-success)]' : 'text-[var(--color-warning)]'}`}>
                {isReady ? 'Capacity is sufficient for admission.' : blockingText}
              </div>
              {!isReady && capacity && <p className="text-[11px] text-[var(--color-text-secondary)]">Regular reserve required: {capacity.occupiedQuarantineStalls + 1} available regular stall(s).</p>}
              {detail.quarantineStallCode && <InfoRow label="Assigned Q stall" value={detail.quarantineStallCode} />}
            </div>
          </Panel>
        </div>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <Panel padded className="bg-[var(--color-surface)]">
            <div className="flex items-center justify-between gap-3">
              <SectionTitle>Documents</SectionTitle>
              <span className="text-[11px] text-[var(--color-text-muted)]">{detail.documents.length} files</span>
            </div>
            {detail.documents.length ? (
              <ul className="mt-4 space-y-2 text-[12px]">
                {detail.documents.map((document) => (
                  <li key={document.id} className="flex items-start gap-2 rounded border border-[var(--color-border)] p-2 transition-colors hover:bg-[var(--color-surface-muted)]">
                    <Icon name={document.documentType === 'HORSE_PHOTO' ? 'image' : 'file-text'} className="mt-0.5 shrink-0" />
                    <div className="min-w-0 flex-1">
                      <a href={admissionsApi.assetUrl(document.fileUrl)} target="_blank" rel="noreferrer" className="block truncate font-medium text-[var(--color-primary)] hover:underline">{document.originalFileName || documentNames[document.documentType] || document.documentType}</a>
                      <p className="mt-0.5 truncate text-[11px] text-[var(--color-text-muted)]">{documentNames[document.documentType] || document.documentType} · Recorded {date(document.recordDate)} · Uploaded {date(document.uploadedAt)}</p>
                    </div>
                    <Icon name="external-link" size={14} className="mt-0.5 shrink-0 text-[var(--color-text-muted)]" />
                  </li>
                ))}
              </ul>
            ) : <p className="mt-4 text-[12px] italic text-[var(--color-text-muted)]">No documents attached.</p>}
          </Panel>

          <Panel padded className="bg-[var(--color-surface)]">
            <SectionTitle>Horse and owner</SectionTitle>
            <dl className="mt-4 space-y-3 text-[12px]">
              <InfoRow label="Horse name" value={candidate.name} />
              <InfoRow label="Owner" value={detail.ownerName ? `${detail.ownerName} (ID ${detail.ownerId})` : `ID ${detail.ownerId}`} />
              <InfoRow label="Horse ID" value={detail.horseId ? String(detail.horseId) : 'Created after approval'} />
              <InfoRow label="Groom feedback" value={detail.groomFeedback} />
            </dl>
          </Panel>
        </div>

        {canReview && (
          <Panel padded className="border-2 border-[var(--color-primary)] bg-[var(--color-surface)]">
            <SectionTitle>Groom decision</SectionTitle>
            <div className="mt-4 space-y-4">
              <label htmlFor={`groom-feedback-${admissionId}`} className="block text-[13px] font-medium text-[var(--color-text-primary)]">Feedback <span className="text-[var(--color-danger)]">*</span></label>
              <textarea id={`groom-feedback-${admissionId}`} value={feedback} onChange={(event) => { setFeedback(event.target.value); setFeedbackError(null); }} rows={4} maxLength={2000} required aria-invalid={Boolean(feedbackError)} className="w-full resize-y rounded-[var(--radius-sm)] border border-[var(--color-border-strong)] bg-[var(--color-surface)] p-3 text-sm text-[var(--color-text-primary)] outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]" />
              <div className="flex justify-between text-xs">
                {feedbackError ? <span role="alert" className="text-[var(--color-danger)]">{feedbackError}</span> : <span className="text-[var(--color-text-muted)]">Required for approval and rejection</span>}
                <span className="text-[var(--color-text-muted)]">{feedback.length}/2000</span>
              </div>
              <div className="flex flex-wrap justify-end gap-2">
                <Button type="button" variant="destructive" size="sm" loading={submitting} disabled={!feedback.trim()} onClick={() => void review('REJECTED')}>Reject</Button>
                <Button type="button" variant="secondary" size="sm" loading={submitting} disabled={!feedback.trim() || isReady} onClick={() => void review('APPROVED')}>Approve &amp; wait</Button>
                <Button type="button" variant="primary" size="sm" loading={submitting} disabled={!feedback.trim() || !isReady} onClick={() => void review('APPROVED')}>Approve</Button>
              </div>
            </div>
          </Panel>
        )}

        {waiting && (
          <Panel padded className="border-2 border-[var(--color-warning)] bg-[var(--color-surface)]">
            <SectionTitle>Waiting for stall</SectionTitle>
            <p className="mt-2 text-[12px] text-[var(--color-text-secondary)]">Groom approved this admission, but capacity was unavailable. Retry when a quarantine stall becomes available.</p>
            <div className="mt-4 flex items-center justify-between gap-3">
              <span className="text-[12px] text-[var(--color-text-secondary)]">Feedback: {detail.groomFeedback || 'Not recorded'}</span>
              <Button type="button" size="sm" loading={submitting} onClick={() => void retryAllocation()} className="bg-[var(--color-warning)] text-white hover:opacity-90">Retry allocation</Button>
            </div>
          </Panel>
        )}

        {!canReview && !waiting && <Panel padded className="bg-[var(--color-surface)]"><p className="text-sm text-[var(--color-text-secondary)]">This application is read-only at its current stage.</p></Panel>}
      </div>
    </div>
  );
}

function prettyStatus(status: AdmissionStatus) {
  return status.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function statusTone(status: AdmissionStatus): 'success' | 'warning' | 'danger' | 'info' | 'primary' | 'neutral' {
  if (status === 'GROOM_REVIEW') return 'primary';
  if (status === 'WAITING_FOR_STALL') return 'warning';
  if (status === 'APPROVED') return 'success';
  if (status === 'REJECTED') return 'danger';
  if (status === 'VET_REVIEW' || status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW') return 'info';
  return 'neutral';
}

function HeaderValue({ label, value }: { label: string; value: string }) {
  return <div><span className="mb-1 block text-[10px] text-[var(--color-text-muted)]">{label}</span><span className="font-medium text-[var(--color-text-primary)]">{value}</span></div>;
}

function InfoRow({ label, value }: { label: string; value: string | null | undefined }) {
  return <div className="flex items-start justify-between gap-4"><span className="shrink-0 text-[var(--color-text-muted)]">{label}</span><span className="text-right font-medium text-[var(--color-text-primary)]">{value || <span className="font-normal italic text-[var(--color-text-muted)]">N/A</span>}</span></div>;
}

function PipelineStep({ label, isDone, isActive }: { label: string; isDone: boolean; isActive: boolean }) {
  if (isActive) return <div className="rounded border border-[var(--color-primary)] bg-[var(--color-primary-soft)] px-3 py-2 font-medium text-[var(--color-primary)]"><span className="mr-1.5">●</span>{label}</div>;
  if (isDone) return <div className="rounded border border-[var(--color-success)] bg-[var(--color-success-soft)] px-3 py-2 text-[var(--color-success)]"><span className="mr-1.5">✓</span>{label}</div>;
  return <div className="rounded border border-[var(--color-border)] bg-[var(--color-surface-subtle)] px-3 py-2 text-[var(--color-text-muted)]"><span className="mr-1.5 opacity-40">-</span>{label}</div>;
}
