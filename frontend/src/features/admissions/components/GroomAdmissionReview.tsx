'use client';

import Link from 'next/link';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/Button';
import { Icon } from '@/components/ui/Icon';
import { FileInput } from '@/components/ui/Input';
import { ImageLightbox, ImageThumbnail } from '@/components/ui/ImageLightbox';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { Notice } from '@/components/ui/Notice';
import { admissionsApi } from '../services/api';
import { formatDate, formatDateTime } from '@/lib/display';
import { ApiError } from '@/services/api';
import { Pill } from '@/components/ui/StatusBadge';
import { ReviewDue } from '../shared/components/ReviewDue';
import { GROOM_FULL_MESSAGE } from '../shared/lib/reviewDue';
import { toast } from '@/lib/toast';
import { ReviewDecisionDialog } from '../shared/components/ReviewDecisionDialog';
import type { ReviewDecisionValue } from '../shared/lib/reviewDecision';
import type { AdmissionDetailResponse } from '../types';
import { AdmissionDetailLayout } from '../shared/components/AdmissionDetailLayout';
import { AdmissionDetailHeader } from '../shared/components/AdmissionDetailHeader';
import { AdmissionPipeline } from '../shared/components/AdmissionPipeline';
import { AdmissionDetailTabs } from '../shared/components/AdmissionDetailTabs';
import { AdmissionDocumentsSection } from '../shared/components/AdmissionDocumentsSection';
import { ArrivalPhotosBlock } from '../shared/components/ArrivalPhotosBlock';
import { AdmissionInfoSection, AdmissionSideCard, InfoRow } from '../shared/components/AdmissionInfoSection';
import { EntityLink } from '@/components/ui/EntityLink';
import { entityHref } from '@/lib/entityRoute';

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
  const [arrivalPhotos, setArrivalPhotos] = useState<{ file: File; url: string }[]>([]);
  const [selectedPhoto, setSelectedPhoto] = useState<number | null>(null);
  const photoUrls = useRef<Set<string>>(new Set());

  useEffect(() => () => {
    photoUrls.current.forEach(URL.revokeObjectURL);
    photoUrls.current.clear();
  }, []);

  const addArrivalPhotos = (files: FileList | null) => {
    if (!files?.length) return;
    const selected = Array.from(files);
    if (selected.some(file => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type))) {
      toast.error('Arrival photos must be JPEG, PNG, or WebP');
      return;
    }
    if (arrivalPhotos.length + selected.length > 5) {
      toast.error('Attach at most 5 arrival photos');
      return;
    }
    const additions = selected.map(file => {
      const url = URL.createObjectURL(file);
      photoUrls.current.add(url);
      return { file, url };
    });
    setArrivalPhotos(current => [...current, ...additions]);
  };

  const removeArrivalPhoto = (url: string) => {
    URL.revokeObjectURL(url);
    photoUrls.current.delete(url);
    setArrivalPhotos(current => current.filter(photo => photo.url !== url));
    setSelectedPhoto(null);
  };

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
      toast.success(choice === 'REJECTED' ? 'Admission rejected' : 'Admission approved');
    } catch (cause) {
      const full = cause instanceof ApiError && cause.status === 409 && cause.errorCode === 'GROOM_FULL';
      const message = full ? GROOM_FULL_MESSAGE : cause instanceof Error ? cause.message : 'Unable to submit the Groom decision.';
      if (full) {
        setDetail(current => current ? { ...current, assignedGroomFull: true } : current);
        setDecision(null);
      }
      setDecisionError(message);
      toast.error(message);
    } finally {
      setSubmitting(false);
    }
  };

  const confirmArrival = async () => {
    setSubmitting(true);
    setError(null);
    try {
      const updated = await admissionsApi.confirmArrival(admissionId, arrivalPhotos.map(photo => photo.file));
      setDetail(updated);
      setConfirmingArrival(false);
      photoUrls.current.forEach(URL.revokeObjectURL);
      photoUrls.current.clear();
      setArrivalPhotos([]);
      onUpdated?.();
      if (updated.status === 'ARRIVAL_EXPIRED') {
        toast.warning('Arrival window expired',
          'The reservation was released. Ask an authorized manager to reopen the admission.');
      } else {
        toast.success('Arrival confirmed');
      }
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Unable to confirm horse arrival.';
      setConfirmingArrival(false);
      setError(message);
      toast.error(message);
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
                      <InfoRow label="Horse name" value={<EntityLink href={detail.horseId ? entityHref('horse', detail.horseId, 'GROOM', { stallCode: detail.quarantineStallCode }) : null}>{candidate.name}</EntityLink>} />
                      <InfoRow label="Owner" value={<EntityLink href={entityHref('owner', detail.ownerId, 'GROOM')}>{detail.ownerName ? `${detail.ownerName} (ID ${detail.ownerId})` : `ID ${detail.ownerId}`}</EntityLink>} />
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
                content: <AdmissionDocumentsSection documents={detail.documents} />,
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
              {detail.quarantineStallCode && <InfoRow label="Assigned Q stall" value={<EntityLink href={entityHref('stall', detail.quarantineStallCode, 'GROOM')}>{detail.quarantineStallCode}</EntityLink>} />}
              {detail.arrivalDeadlineAt && <InfoRow label="Arrival deadline" value={formatDateTime(detail.arrivalDeadlineAt)} />}
              <Notice tone={isReady ? 'success' : 'warning'}>
                {isReady ? 'Capacity is sufficient for admission.' : blockingText}
                {!isReady && capacity && (
                  <span className="mt-1 block text-xs">Regular reserve required: {capacity.occupiedQuarantineStalls + 1} available regular stall(s).</span>
                )}
              </Notice>
            </AdmissionSideCard>

            {detail.arrivedAt && (
              <AdmissionSideCard title="Arrival">
                <InfoRow label="Checked in at" value={formatDateTime(detail.arrivedAt)} />
                <ArrivalPhotosBlock documents={detail.documents} />
              </AdmissionSideCard>
            )}

            {canReview && (
              <AdmissionSideCard title="Groom decision" tone="primary" className="space-y-3">
                <InfoRow label="Review due" value={<ReviewDue dueAt={detail.groomReviewDueAt} />} />
                {detail.assignedGroomFull && <Pill tone="warning" size="sm">Full</Pill>}
                {detail.assignedGroomFull && <Notice tone="warning">{GROOM_FULL_MESSAGE}</Notice>}
                <div className="space-y-2">
                  <Button type="button" variant="primary" className="w-full" disabled={submitting || detail.assignedGroomFull} onClick={() => setDecision('APPROVED')}>Approve</Button>
                  <Button type="button" variant="destructive" className="w-full" disabled={submitting} onClick={() => setDecision('REJECTED')}>Reject</Button>
                </div>
              </AdmissionSideCard>
            )}

            {waiting && (
              <AdmissionSideCard title="Queued for a quarantine stall">
                <p className="text-[var(--color-text-secondary)]">
                  Approved and waiting for capacity. The system assigns the next free quarantine stall automatically,
                  in submission order. No action is needed.
                </p>
                <InfoRow label="Feedback" value={detail.groomFeedback || 'Not recorded'} />
              </AdmissionSideCard>
            )}

            {detail.status === 'WAITING_FOR_ARRIVAL' && (
              <AdmissionSideCard title="Confirm horse arrival" tone="primary">
                <FileInput
                  accept="image/jpeg,image/png,image/webp"
                  multiple
                  aria-label="Arrival photos"
                  disabled={submitting}
                  onChange={event => {
                    addArrivalPhotos(event.currentTarget.files);
                    event.currentTarget.value = '';
                  }}
                />
                <span className="text-sm text-[var(--color-text-secondary)]">{arrivalPhotos.length}/5 photos</span>
                {arrivalPhotos.length > 0 && (
                  <div className="grid grid-cols-3 gap-2">
                    {arrivalPhotos.map((photo, index) => (
                      <div key={photo.url} className="relative">
                        <ImageThumbnail src={photo.url} alt={`Arrival photo ${index + 1}`} onClick={() => setSelectedPhoto(index)} />
                        <Button type="button" variant="secondary" size="sm" aria-label={`Remove arrival photo ${index + 1}`} onClick={() => removeArrivalPhoto(photo.url)} className="absolute right-1 top-1 p-1"><Icon name="x" size={14} /></Button>
                      </div>
                    ))}
                  </div>
                )}
                <Button type="button" variant="primary" className="w-full" disabled={submitting || arrivalPhotos.length === 0} onClick={() => setConfirmingArrival(true)}>Mark horse arrived</Button>
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

      <ReviewDecisionDialog
        decision={decision}
        title={decision === 'REJECTED' ? 'Reject admission?' : 'Approve admission?'}
        loading={submitting}
        error={decisionError}
        onConfirm={(choice, feedback) => void review(choice, feedback)}
        onClose={closeDecision}
      />

      <ConfirmDialog
        open={confirmingArrival}
        title="Confirm the horse has arrived?"
        confirmLabel="Mark arrived"
        loading={submitting}
        onConfirm={() => void confirmArrival()}
        onCancel={() => { if (!submitting) setConfirmingArrival(false); }}
      />
      <ImageLightbox
        images={arrivalPhotos.map((photo, index) => ({ src: photo.url, alt: `Arrival photo ${index + 1}` }))}
        index={selectedPhoto}
        onIndexChange={setSelectedPhoto}
        onClose={() => setSelectedPhoto(null)}
      />
    </div>
  );
}
