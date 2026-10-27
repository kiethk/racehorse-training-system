'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, EmptyState, HorseAvatar, Icon, ListSkeleton, Panel, Pill, SectionTitle, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError, formatDate, formatDateTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { AdmissionDetailLayout } from '@/features/admissions/shared/components/AdmissionDetailLayout';
import { AdmissionDetailTabs } from '@/features/admissions/shared/components/AdmissionDetailTabs';
import { AdmissionInfoSection, AdmissionSideCard, InfoRow } from '@/features/admissions/shared/components/AdmissionInfoSection';
import { ReviewDecisionDialog } from '@/features/admissions/shared/components/ReviewDecisionDialog';
import type { ReviewDecisionValue } from '@/features/admissions/shared/lib/reviewDecision';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { diffFields } from '../lib/horseEdit';
import { DocumentSlotGrid } from './DocumentSlotGrid';
import { EditRequestStatusBadge } from './GroomHorseEditRequestsView';
import type { HorseDocumentSlot, HorseEditRequest } from '../types';

function HeaderValue({ label, value }: { label: string; value: string }) {
  return (
    <div className="min-w-0">
      <span className="mb-0.5 block text-xs font-medium uppercase tracking-wide text-[var(--color-text-muted)]">{label}</span>
      <span className="break-words text-sm font-medium text-[var(--color-text-primary)]">{value}</span>
    </div>
  );
}

/** Same header as an admission: avatar, name with status, then the key facts. */
function RequestHeader({ request }: { request: HorseEditRequest }) {
  const name = request.horseName ?? `Horse #${request.horseId}`;
  return (
    <header className="flex min-w-0 shrink-0 items-start gap-3 border-b border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:items-center sm:gap-4 sm:px-6 sm:py-5">
      <HorseAvatar name={name} image={`/api/horses/${request.horseId}/photo`} size={56} rounded="md" />
      <div className="min-w-0 flex-1">
        <h1 className="mb-2.5 flex flex-wrap items-center gap-2.5 text-xl font-semibold tracking-tight text-[var(--color-text-primary)]">
          <span className="truncate">{name}</span>
          <EditRequestStatusBadge status={request.status} />
        </h1>
        <div className="grid min-w-0 grid-cols-1 gap-x-5 gap-y-2 min-[480px]:grid-cols-2 lg:grid-cols-4">
          <HeaderValue label="Request ID" value={`#${request.id}`} />
          <HeaderValue label="Owner" value={request.ownerName ? `${request.ownerName} (#${request.ownerId})` : `#${request.ownerId}`} />
          <HeaderValue label="UELN" value={request.horseRegistrationNumber || 'Not recorded'} />
          <HeaderValue label="Submitted" value={formatDate(request.createdAt)} />
        </div>
      </div>
    </header>
  );
}

type StepState = 'completed' | 'current' | 'pending' | 'rejected';

/** Submitted → Groom review → Applied, drawn like the admission review pipeline. */
function RequestProgress({ request }: { request: HorseEditRequest }) {
  const decided = request.status !== 'PENDING';
  const last: { label: string; state: StepState; icon: 'check' | 'x' } =
    request.status === 'APPROVED' ? { label: 'Applied', state: 'completed', icon: 'check' }
      : request.status === 'REJECTED' ? { label: 'Rejected', state: 'rejected', icon: 'x' }
        : request.status === 'CANCELLED' ? { label: 'Cancelled by owner', state: 'rejected', icon: 'x' }
          : { label: 'Applied', state: 'pending', icon: 'check' };
  const steps: { label: string; state: StepState; note: string | null; icon: 'file-text' | 'clipboard' | 'check' | 'x' }[] = [
    { label: 'Submitted', state: 'completed', note: formatDateTime(request.createdAt), icon: 'file-text' },
    {
      label: 'Groom review',
      state: decided ? (request.status === 'CANCELLED' ? 'pending' : 'completed') : 'current',
      note: request.reviewedAt ? formatDateTime(request.reviewedAt) : decided ? null : 'Waiting for your decision',
      icon: 'clipboard',
    },
    { label: last.label, state: last.state, note: null, icon: last.icon },
  ];
  const tone: Record<StepState, string> = {
    completed: 'border-[var(--color-success)] bg-[var(--color-success-soft)] text-[var(--color-success)]',
    current: 'border-[var(--color-primary)] bg-[var(--color-primary-soft)] text-[var(--color-primary)]',
    pending: 'border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)]',
    rejected: 'border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)]',
  };
  return (
    <Panel padded className="space-y-4">
      <SectionTitle>Review progress</SectionTitle>
      <ol className="grid grid-cols-3 gap-2">
        {steps.map((step) => (
          <li key={step.label} className="flex flex-col items-center gap-2 text-center">
            <span className={cn('flex h-12 w-12 items-center justify-center rounded-full border-2', tone[step.state])}>
              <Icon name={step.icon} size={20} />
            </span>
            <span className={cn('text-xs font-semibold', step.state === 'pending' ? 'text-[var(--color-text-muted)]' : 'text-[var(--color-text-primary)]')}>
              {step.label}
            </span>
            {step.note && <span className="text-xs text-[var(--color-text-secondary)]">{step.note}</span>}
          </li>
        ))}
      </ol>
    </Panel>
  );
}

/** Groom review of one change request, laid out like the groom admission review. */
export function GroomHorseEditRequestReview({ requestId }: { requestId: number }) {
  const [request, setRequest] = useState<HorseEditRequest | null>(null);
  const [currentSlots, setCurrentSlots] = useState<HorseDocumentSlot[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [decision, setDecision] = useState<ReviewDecisionValue | null>(null);
  const [decisionError, setDecisionError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const loaded = await horseEditRequestService.getForGroom(requestId);
      setRequest(loaded);
      setCurrentSlots(await horseEditRequestService.documents(loaded.horseId).catch(() => []));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load this change request.'));
    } finally {
      setLoading(false);
    }
  }, [requestId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void load();
  }, [load]);

  const review = async (choice: ReviewDecisionValue, feedback: string | undefined) => {
    setSubmitting(true);
    setDecisionError(null);
    try {
      const updated = await horseEditRequestService.review(requestId, choice, feedback);
      setRequest(updated);
      setDecision(null);
      if (choice === 'APPROVED') {
        setCurrentSlots(await horseEditRequestService.documents(updated.horseId).catch(() => currentSlots));
        toast.success('Changes approved', `${updated.horseName ?? 'The horse'} now shows the updated details.`);
      } else {
        toast.success('Change request rejected', 'The owner can see your reason.');
      }
    } catch (cause) {
      const message = displayError(cause, 'Unable to save your decision.');
      setDecisionError(message);
      toast.error('Decision not saved', message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) return <div className="p-6"><ListSkeleton rows={8} /></div>;
  if (error || !request) {
    return <EmptyState icon="alert-triangle" title="Unable to load change request" description={error ?? 'Not found.'}
      action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />;
  }

  const changes = diffFields(request.original, request.requested);
  const pending = request.status === 'PENDING';
  const requestedSlots: HorseDocumentSlot[] = request.documents.map((doc) => ({
    documentType: doc.documentType,
    source: 'EDIT_REQUEST',
    documentId: doc.id,
    originalFileName: doc.originalFileName,
    fileUrl: doc.fileUrl,
    updatedAt: request.createdAt,
  }));
  const requestedTypes = new Set(request.documents.map((doc) => doc.documentType));

  return (
    <div className="space-y-5">
      <AdmissionDetailLayout
        returnTo="/groom/horse-edit-requests"
        header={<RequestHeader request={request} />}
        pipeline={<RequestProgress request={request} />}
        content={(
          <AdmissionDetailTabs
            tabs={[
              {
                id: 'changes',
                label: 'Detail changes',
                count: changes.length,
                content: (
                  <AdmissionInfoSection title="Horse details">
                    {changes.length === 0 ? (
                      <p className="text-sm text-[var(--color-text-muted)]">No detail changes; this request only updates documents.</p>
                    ) : (
                      <Table bare>
                        <THead><Tr><Th>Field</Th><Th>Current</Th><Th>Requested</Th></Tr></THead>
                        <TBody>
                          {changes.map((change) => (
                            <Tr key={change.field}>
                              <Td className="font-medium">{change.label}</Td>
                              <Td className="text-[var(--color-text-muted)]">{change.from ?? 'Not recorded'}</Td>
                              <Td>
                                <span className="font-medium text-[var(--color-text-primary)]">{change.to ?? 'Not recorded'}</span>
                                <span className="ml-2"><Pill tone="info" size="sm">Changed</Pill></span>
                              </Td>
                            </Tr>
                          ))}
                        </TBody>
                      </Table>
                    )}
                  </AdmissionInfoSection>
                ),
              },
              {
                id: 'documents',
                label: 'Documents',
                count: request.documents.length,
                content: request.documents.length === 0 ? (
                  <AdmissionInfoSection title="Documents">
                    <p className="text-sm text-[var(--color-text-muted)]">No documents in this request.</p>
                  </AdmissionInfoSection>
                ) : (
                  <>
                    <AdmissionInfoSection title="Requested documents">
                      <DocumentSlotGrid slots={requestedSlots.filter((slot) => requestedTypes.has(slot.documentType))} onlyTypes={requestedTypes} />
                    </AdmissionInfoSection>
                    <AdmissionInfoSection title={pending ? 'Currently on file' : 'On file now'}>
                      <DocumentSlotGrid slots={currentSlots} onlyTypes={requestedTypes} />
                    </AdmissionInfoSection>
                  </>
                ),
              },
            ]}
          />
        )}
        sidebar={pending ? (
          <AdmissionSideCard title="Groom decision" tone="primary" className="space-y-3">
            <p className="text-[var(--color-text-secondary)]">
              Approving applies these changes to the horse right away and replaces the listed documents.
              Rejecting keeps the horse as it is and discards the uploaded files.
            </p>
            <div className="space-y-2">
              <Button type="button" variant="primary" className="w-full" disabled={submitting} onClick={() => setDecision('APPROVED')}>Approve</Button>
              <Button type="button" variant="destructive" className="w-full" disabled={submitting} onClick={() => setDecision('REJECTED')}>Reject</Button>
            </div>
          </AdmissionSideCard>
        ) : (
          <AdmissionSideCard title="Decision">
            <InfoRow label="Status" value={<EditRequestStatusBadge status={request.status} />} />
            {request.reviewedAt && <InfoRow label="Reviewed" value={formatDateTime(request.reviewedAt)} />}
            {request.reviewerName && <InfoRow label="Reviewed by" value={request.reviewerName} />}
            <InfoRow label="Feedback" value={request.reviewFeedback || 'Not recorded'} />
          </AdmissionSideCard>
        )}
      />

      <ReviewDecisionDialog
        decision={decision}
        title={decision === 'REJECTED' ? 'Reject these changes?' : 'Approve these changes?'}
        description={decision === 'REJECTED'
          ? 'Nothing changes on the horse and the uploaded files are discarded. The owner sees your reason.'
          : 'The horse record and documents are updated right away.'}
        loading={submitting}
        error={decisionError}
        onConfirm={(choice, feedback) => void review(choice, feedback)}
        onClose={() => { if (!submitting) { setDecision(null); setDecisionError(null); } }}
      />
    </div>
  );
}
