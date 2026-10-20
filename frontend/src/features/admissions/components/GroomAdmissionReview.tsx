'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { FormField } from '@/components/ui/FormField';
import { Textarea } from '@/components/ui/Input';
import { Notice } from '@/components/ui/Notice';
import { admissionsApi } from '../services/api';
import { formatDate, formatDateTime } from '@/lib/display';
import type { AdmissionDetailResponse } from '../types';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionDetailTabs } from '../shared/components/AdmissionDetailTabs';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { AdmissionInfoSection, AdmissionSideCard, InfoRow } from '../shared/components/AdmissionInfoSection';

function date(value: string | null) {
  return value ? formatDate(value) : 'Not recorded';
}

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
    <div className="space-y-5">
      {notice && <Notice tone="success">{notice}</Notice>}
      {error && detail && <Notice tone="error">{error}</Notice>}

      <AdmissionDetailLayout
        returnTo={returnTo}
        header={<AdmissionDetailHeader detail={detail} horsePhotoUrl={horsePhoto ? admissionsApi.assetUrl(horsePhoto.fileUrl) : undefined} />}
        pipeline={<AdmissionPipeline detail={detail} />}
        content={(
          <AdmissionDetailTabs
            tabs={[
              {
                id: 'overview',
                label: 'Overview',
                content: (
                  <>
                    <AdmissionInfoSection title="Horse and owner">
                      <InfoRow label="Horse name" value={candidate.name} />
                      <InfoRow label="Owner" value={detail.ownerName ? `${detail.ownerName} (ID ${detail.ownerId})` : `ID ${detail.ownerId}`} />
                      <InfoRow label="Horse ID" value={detail.horseId ? String(detail.horseId) : 'Created after arrival confirmation'} />
                      <InfoRow label="Groom feedback" value={detail.groomFeedback} />
                    </AdmissionInfoSection>
                    <AdmissionInfoSection title="Pedigree & Registration">
                      <InfoRow label="Registry name" value={candidate.registryName} />
                      <InfoRow label="Registration no." value={candidate.registrationNumber} />
                      <InfoRow label="Date of birth" value={date(candidate.dateOfBirth)} />
                      <div className="space-y-3 border-t border-[var(--color-border)] pt-3">
                        <InfoRow label="Sire" value={candidate.sireName} />
                        <InfoRow label="Dam" value={candidate.damName} />
                        {candidate.pedigreeNotes && <p className="italic text-[var(--color-text-secondary)]">&quot;{candidate.pedigreeNotes}&quot;</p>}
                      </div>
                    </AdmissionInfoSection>
                  </>
                ),
              },
              {
                id: 'documents',
                label: 'Documents',
                count: detail.documents.length,
                content: <AdmissionDocumentsSection documents={detail.documents} assetUrl={admissionsApi.assetUrl} />,
              },
            ]}
          />
        )}
        sidebar={(
          <>
            <AdmissionSideCard title="Admission capacity">
              <InfoRow label="Available quarantine stalls" value={String(capacity?.availableQuarantineStalls ?? '—')} />
              <InfoRow label="Available regular stalls" value={String(capacity?.availableRegularStalls ?? '—')} />
              <InfoRow label="Occupied quarantine stalls" value={String(capacity?.occupiedQuarantineStalls ?? '—')} />
              {detail.quarantineStallCode && <InfoRow label="Assigned Q stall" value={detail.quarantineStallCode} />}
              {detail.arrivalDeadlineAt && <InfoRow label="Arrival deadline" value={formatDateTime(detail.arrivalDeadlineAt)} />}
              <Notice tone={isReady ? 'success' : 'warning'}>
                {isReady ? 'Capacity is sufficient for admission.' : blockingText}
                {!isReady && capacity && (
                  <span className="mt-1 block text-xs">Regular reserve required: {capacity.occupiedQuarantineStalls + 1} available regular stall(s).</span>
                )}
              </Notice>
            </AdmissionSideCard>

            {canReview && (
              <AdmissionSideCard title="Groom decision" tone="primary" className="space-y-4">
                <FormField
                  label="Feedback"
                  required
                  error={feedbackError ?? undefined}
                  hint={`Required for approval and rejection · ${feedback.length}/2000`}
                >
                  <Textarea
                    value={feedback}
                    onChange={(event) => { setFeedback(event.target.value); setFeedbackError(null); }}
                    rows={4}
                    maxLength={2000}
                  />
                </FormField>
                <div className="space-y-2">
                  <Button type="button" variant="primary" className="w-full" loading={submitting} disabled={!feedback.trim() || !isReady} onClick={() => void review('APPROVED')}>Approve</Button>
                  <Button type="button" variant="secondary" className="w-full" loading={submitting} disabled={!feedback.trim() || isReady} onClick={() => void review('APPROVED')}>Approve &amp; wait for stall</Button>
                  <Button type="button" variant="destructive" className="w-full" loading={submitting} disabled={!feedback.trim()} onClick={() => void review('REJECTED')}>Reject</Button>
                </div>
              </AdmissionSideCard>
            )}

            {waiting && (
              <AdmissionSideCard title="Waiting for stall" tone="warning">
                <p className="text-[var(--color-text-secondary)]">Groom approved this admission, but capacity was unavailable. Retry when a quarantine stall becomes available.</p>
                <InfoRow label="Feedback" value={detail.groomFeedback || 'Not recorded'} />
                <Button type="button" variant="warning" className="w-full" loading={submitting} onClick={() => void retryAllocation()}>Retry allocation</Button>
              </AdmissionSideCard>
            )}

            {detail.status === 'WAITING_FOR_ARRIVAL' && (
              <AdmissionSideCard title="Confirm horse arrival" tone="primary">
                <p className="text-[var(--color-text-secondary)]">
                  Quarantine stall {detail.quarantineStallCode || 'reserved'} is held until {detail.arrivalDeadlineAt ? formatDateTime(detail.arrivalDeadlineAt) : 'the 14-day arrival deadline'}.
                  Confirm only after the horse physically arrives. Confirmation creates the horse record and schedules veterinary review.
                </p>
                <Button type="button" variant="primary" className="w-full" loading={submitting} onClick={() => void confirmArrival()}>Mark horse arrived</Button>
              </AdmissionSideCard>
            )}

            {detail.status === 'ARRIVAL_EXPIRED' && (
              <AdmissionSideCard title="Arrival window expired" tone="warning">
                <p className="text-[var(--color-text-secondary)]">The reserved quarantine stall was released after 14 days. An authorized manager can reopen the arrival window if needed.</p>
              </AdmissionSideCard>
            )}

            {!canReview && !waiting && detail.status !== 'WAITING_FOR_ARRIVAL' && detail.status !== 'ARRIVAL_EXPIRED' && (
              <AdmissionSideCard title="Groom decision">
                <p className="text-[var(--color-text-secondary)]">This application is read-only at its current stage.</p>
              </AdmissionSideCard>
            )}
          </>
        )}
      />
    </div>
  );
}
