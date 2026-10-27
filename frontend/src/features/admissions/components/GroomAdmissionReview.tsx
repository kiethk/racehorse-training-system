'use client';

import Link from 'next/link';
import { useCallback, useEffect, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { admissionsApi } from '../services/api';
import { formatDate, formatDateTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { ReviewDecisionDialog } from '../shared/components/ReviewDecisionDialog';
import type { ReviewDecisionValue } from '../shared/lib/reviewDecision';
import type { AdmissionDetailResponse, GroomApproveBlockedReason } from '../types';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionDetailTabs } from '../shared/components/AdmissionDetailTabs';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { AdmissionInfoSection, AdmissionSideCard, InfoRow } from '../shared/components/AdmissionInfoSection';

const APPROVE_BLOCKED_TEXT: Record<GroomApproveBlockedReason, string> = {
  NOT_IN_GROOM_POOL: 'This admission is not in your review pool right now.',
  OLDER_ADMISSION_FIRST: 'Review older admissions first. Approvals follow submission order.',
  NO_QUARANTINE_STALL: 'You have no free quarantine stall.',
  REGULAR_RESERVE_INSUFFICIENT: 'Regular-stall reserve is below the required quarantine turnover reserve.',
};

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
  const [submitting, setSubmitting] = useState(false);
  const [decision, setDecision] = useState<ReviewDecisionValue | null>(null);
  const [decisionError, setDecisionError] = useState<string | null>(null);
  const [confirmingArrival, setConfirmingArrival] = useState(false);

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

  const closeDecision = () => {
    if (submitting) return;
    setDecision(null);
    setDecisionError(null);
  };

  const review = async (choice: ReviewDecisionValue, feedback: string | undefined) => {
    setSubmitting(true);
    setDecisionError(null);
    try {
      const updated = await admissionsApi.groomReview(admissionId, { decision: choice, feedback });
      setDetail(updated);
      setDecision(null);
      onUpdated?.();
      if (choice === 'REJECTED') {
        toast.success('Admission rejected', 'The owner can see your reason on the application.');
      } else {
        toast.success('Admission approved',
          `Quarantine stall ${updated.quarantineStallCode ?? ''} is reserved for you until ${updated.arrivalDeadlineAt ? formatDateTime(updated.arrivalDeadlineAt) : 'the arrival deadline'}.`);
      }
    } catch (cause) {
      // The pool may have moved since the page loaded (another groom approved first); reload what this groom may do now.
      const message = cause instanceof Error ? cause.message : 'Unable to submit the Groom decision.';
      toast.error('Decision not saved', message);
      setDecision(null);
      void load();
    } finally {
      setSubmitting(false);
    }
  };

  const confirmArrival = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const updated = await admissionsApi.confirmArrival(admissionId);
      setDetail(updated);
      setConfirmingArrival(false);
      onUpdated?.();
      if (updated.status === 'ARRIVAL_EXPIRED') {
        toast.warning('Arrival window expired',
          'The reservation was released; an authorized manager can return the admission to groom review.');
      } else {
        toast.success('Arrival confirmed',
          'The horse was registered and the initial veterinary exam is being scheduled.');
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Unable to confirm horse arrival.';
      setConfirmingArrival(false);
      setError(message);
      toast.error('Arrival not confirmed', message);
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
  const eligibility = detail.groomReviewEligibility ?? null;
  const canApprove = eligibility?.canApprove ?? false;
  const canReject = eligibility?.canReject ?? true;
  const approveBlockedText = eligibility?.approveBlockedReason
    ? APPROVE_BLOCKED_TEXT[eligibility.approveBlockedReason]
    : null;
  const capacity = detail.capacity;
  const isReady = capacity?.admissionCapacityAvailable ?? false;
  const blockingText = capacity?.blockingReason === 'NO_QUARANTINE_STALL'
    ? 'No quarantine stall is available.'
    : capacity?.blockingReason === 'REGULAR_RESERVE_INSUFFICIENT'
      ? 'Regular-stall reserve is below the required quarantine turnover reserve.'
      : 'The current capacity snapshot could not be loaded.';

  return (
    <div className="space-y-5">
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
              <AdmissionSideCard title="Groom decision" tone="primary" className="space-y-3">
                <p className="text-[var(--color-text-secondary)]">
                  {canApprove && eligibility?.targetStallCode
                    ? `Approving reserves your quarantine stall ${eligibility.targetStallCode} for this horse. You will confirm its arrival.`
                    : 'Approving reserves one of your own quarantine stalls for this horse. You will confirm its arrival.'}
                </p>
                {approveBlockedText && <Notice tone="warning">{approveBlockedText}</Notice>}
                <div className="space-y-2">
                  <Button type="button" variant="primary" className="w-full" disabled={submitting || !canApprove} onClick={() => setDecision('APPROVED')}>Approve</Button>
                  {canReject && (
                    <Button type="button" variant="destructive" className="w-full" disabled={submitting} onClick={() => setDecision('REJECTED')}>Reject</Button>
                  )}
                </div>
              </AdmissionSideCard>
            )}

            {detail.status === 'WAITING_FOR_ARRIVAL' && (
              <AdmissionSideCard title="Confirm horse arrival" tone="primary">
                <p className="text-[var(--color-text-secondary)]">
                  Quarantine stall {detail.quarantineStallCode || 'reserved'} is held until {detail.arrivalDeadlineAt ? formatDateTime(detail.arrivalDeadlineAt) : 'the 14-day arrival deadline'}.
                  Confirm only after the horse physically arrives. Confirmation creates the horse record and schedules veterinary review.
                </p>
                <Button type="button" variant="primary" className="w-full" disabled={submitting} onClick={() => setConfirmingArrival(true)}>Mark horse arrived</Button>
              </AdmissionSideCard>
            )}

            {detail.status === 'ARRIVAL_EXPIRED' && (
              <AdmissionSideCard title="Arrival window expired" tone="warning">
                <p className="text-[var(--color-text-secondary)]">The reserved quarantine stall was released after 14 days. An authorized manager can return it to groom review if needed.</p>
              </AdmissionSideCard>
            )}

            {!canReview && detail.status !== 'WAITING_FOR_ARRIVAL' && detail.status !== 'ARRIVAL_EXPIRED' && (
              <AdmissionSideCard title="Groom decision">
                <p className="text-[var(--color-text-secondary)]">This application is read-only at its current stage.</p>
              </AdmissionSideCard>
            )}
          </>
        )}
      />

      <ReviewDecisionDialog
        decision={decision}
        title={decision === 'REJECTED' ? 'Reject admission?' : 'Approve admission?'}
        description={decision === 'REJECTED'
          ? 'The application closes and the owner sees your reason.'
          : `Your quarantine stall ${eligibility?.targetStallCode ?? ''} is reserved for this horse for 14 days, and you confirm its arrival.`}
        loading={submitting}
        error={decisionError}
        onConfirm={(choice, feedback) => void review(choice, feedback)}
        onClose={closeDecision}
      />

      <ConfirmDialog
        open={confirmingArrival}
        title="Confirm the horse has arrived?"
        description="This creates the horse record in the reserved quarantine stall and schedules the initial veterinary exam. It cannot be undone."
        confirmLabel="Mark arrived"
        loading={submitting}
        onConfirm={() => void confirmArrival()}
        onCancel={() => { if (!submitting) setConfirmingArrival(false); }}
      />
    </div>
  );
}
