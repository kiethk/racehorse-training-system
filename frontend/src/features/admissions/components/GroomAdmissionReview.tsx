'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { admissionsApi } from '../services/api';
import { formatDate, formatDateTime } from '@/lib/display';
import type { AdmissionDetailResponse } from '../types';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';

function date(value: string | null) {
  return value ? formatDate(value) : 'Not recorded';
}

const documentNames: Record<string, string> = {
  HORSE_PHOTO: 'Horse photo',
  REGISTRATION_DOCUMENT: 'Registration document',
  PEDIGREE_CERTIFICATE: 'Horse birth certificate',
  VACCINATION_RECORD: 'Vaccination record',
  DEWORMING_RECORD: 'Deworming record',
  HEALTH_CERTIFICATE: 'Health certificate',
  PREVIOUS_MEDICAL_RECORD: 'Previous medical record',
  PREVIOUS_INJURY_RECORD: 'Previous injury record',
};

interface Props {
  admissionId: number;
  returnTo: string;
  onUpdated?: () => void;
}

export function GroomAdmissionReview({ admissionId, returnTo, onUpdated }: Props) {
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
    setDetail(null);
    try {
      setDetail(await admissionsApi.getAdmissionDetail(admissionId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to load this application.');
    } finally {
      setLoading(false);
    }
  }, [admissionId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

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
        : decision === 'REJECTED' ? 'Application rejected.' : 'Application approved. A quarantine stall is reserved for 14 days while waiting for arrival.');
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
        : 'A quarantine stall is reserved for 14 days while waiting for arrival.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to retry stall allocation.');
    } finally {
      setSubmitting(false);
    }
  };

  const confirmArrival = async () => {
    setSubmitting(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await admissionsApi.confirmArrival(admissionId);
      setDetail(updated);
      onUpdated?.();
      setNotice(updated.status === 'ARRIVAL_EXPIRED'
        ? 'The arrival deadline has passed. The reservation was released; ask an authorized manager to reopen the admission.'
        : 'Arrival confirmed and the horse was registered. Veterinary assignment is being scheduled with workload and overdue appointments taken into account.');
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Unable to confirm horse arrival.');
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
    <AdmissionDetailLayout
      returnTo={returnTo}
      header={<AdmissionDetailHeader detail={detail} horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} />}
      pipeline={<AdmissionPipeline detail={detail} />}
      content={(
      <div className="min-w-0 space-y-5">
        {notice && <div role="status" className="border-l-2 border-[var(--color-success)] bg-[var(--color-success-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{notice}</div>}
        {error && detail && <div role="alert" className="border-l-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-4 py-3 text-sm text-[var(--color-text-primary)]">{error}</div>}

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
              {detail.arrivalDeadlineAt && <InfoRow label="Arrival deadline" value={formatDateTime(detail.arrivalDeadlineAt)} />}
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
                  <li key={document.id} className="flex items-center gap-3 rounded border border-[var(--color-border)] p-2 transition-colors hover:bg-[var(--color-surface-muted)]">
                    <DocumentTypeMark documentType={document.documentType} />
                    <div className="min-w-0 flex-1">
                      <a href={admissionsApi.assetUrl(document.fileUrl)} target="_blank" rel="noreferrer" className="block truncate font-medium text-[var(--color-primary)] hover:underline">{document.originalFileName || documentNames[document.documentType] || document.documentType}</a>
                      <p className="mt-0.5 truncate text-[11px] text-[var(--color-text-muted)]">{documentNames[document.documentType] || document.documentType} · Recorded {date(document.recordDate)} · Uploaded {date(document.uploadedAt)}</p>
                    </div>
                    <a href={admissionsApi.assetUrl(document.fileUrl)} target="_blank" rel="noreferrer" className="shrink-0 rounded-[var(--radius-sm)] px-2 py-1 text-[11px] font-medium text-[var(--color-primary)] hover:bg-[var(--color-primary-soft)]">View</a>
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
              <InfoRow label="Horse ID" value={detail.horseId ? String(detail.horseId) : 'Created after arrival confirmation'} />
              <InfoRow label="Groom feedback" value={detail.groomFeedback} />
            </dl>
          </Panel>
        </div>
      </div>
      )}
      actions={(
        <div className="space-y-4">

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

        {detail.status === 'WAITING_FOR_ARRIVAL' && (
          <Panel padded className="border-2 border-[var(--color-primary)] bg-[var(--color-surface)]">
            <SectionTitle>Confirm horse arrival</SectionTitle>
            <p className="mt-2 text-[12px] text-[var(--color-text-secondary)]">
              Quarantine stall {detail.quarantineStallCode || 'reserved'} is held until {detail.arrivalDeadlineAt ? formatDateTime(detail.arrivalDeadlineAt) : 'the 14-day arrival deadline'}.
              Confirm only after the horse physically arrives. Confirmation creates the horse record and schedules veterinary review.
            </p>
            <div className="mt-4 flex justify-end">
              <Button type="button" size="sm" loading={submitting} onClick={() => void confirmArrival()}>Mark horse arrived</Button>
            </div>
          </Panel>
        )}

        {detail.status === 'ARRIVAL_EXPIRED' && (
          <Panel padded className="border border-[var(--color-warning)] bg-[var(--color-surface)]">
            <SectionTitle>Arrival window expired</SectionTitle>
            <p className="mt-2 text-[12px] text-[var(--color-text-secondary)]">The reserved quarantine stall was released after 14 days. An authorized manager can reopen the arrival window if needed.</p>
          </Panel>
        )}

        {!canReview && !waiting && detail.status !== 'WAITING_FOR_ARRIVAL' && detail.status !== 'ARRIVAL_EXPIRED' && <Panel padded className="bg-[var(--color-surface)]"><p className="text-sm text-[var(--color-text-secondary)]">This application is read-only at its current stage.</p></Panel>}
      </div>
      )}
    />
  );
}

function InfoRow({ label, value }: { label: string; value: string | null | undefined }) {
  return <div className="flex items-start justify-between gap-4"><span className="shrink-0 text-[var(--color-text-muted)]">{label}</span><span className="text-right font-medium text-[var(--color-text-primary)]">{value || <span className="font-normal italic text-[var(--color-text-muted)]">N/A</span>}</span></div>;
}

function DocumentTypeMark({ documentType }: { documentType: string }) {
  const isPhoto = documentType === 'HORSE_PHOTO';
  return (
    <span className="flex h-9 w-10 shrink-0 items-center justify-center rounded-[var(--radius-xs)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] text-[9px] font-semibold tracking-wide text-[var(--color-text-muted)]">
      {isPhoto ? 'PHOTO' : 'FILE'}
    </span>
  );
}
