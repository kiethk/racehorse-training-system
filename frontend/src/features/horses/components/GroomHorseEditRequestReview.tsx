'use client';

import { useCallback, useEffect, useState } from 'react';
import {
  BackLink,
  Button,
  DetailSkeleton,
  EmptyState,
  Notice,
  Panel,
  Pill,
  ScreenLayout,
  Table,
  TBody,
  Td,
  Th,
  THead,
  Tr,
} from '@/components/ui';
import { displayError, formatDateTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { ReviewDecisionDialog } from '@/features/admissions/shared/components/ReviewDecisionDialog';
import type { ReviewDecisionValue } from '@/features/admissions/shared/lib/reviewDecision';
import { horseEditRequestService } from '../services/horseEditRequestService';
import { diffFields } from '../lib/horseEdit';
import { DocumentSlotGrid } from './DocumentSlotGrid';
import { EDIT_STATUS_LABEL, EDIT_STATUS_TONE } from './GroomHorseEditRequestsView';
import type { HorseDocumentSlot, HorseEditRequest } from '../types';

/** Groom review of one change request: what changes (current vs requested) and the files to replace. */
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

  if (loading) return <ScreenLayout variant="detail"><DetailSkeleton /></ScreenLayout>;
  if (error || !request) {
    return (
      <ScreenLayout variant="detail">
        <EmptyState icon="alert-triangle" title="Unable to open change request" description={error ?? 'Not found.'}
          action={<Button size="sm" onClick={() => void load()}>Retry</Button>} />
      </ScreenLayout>
    );
  }

  const changes = diffFields(request.original, request.requested);
  const pending = request.status === 'PENDING';
  // What the documents would look like after approval: requested files over the current ones.
  const requestedSlots: HorseDocumentSlot[] = request.documents.map((doc) => ({
    documentType: doc.documentType,
    source: 'EDIT_REQUEST',
    documentId: doc.id,
    originalFileName: doc.originalFileName,
    fileUrl: doc.fileUrl,
    updatedAt: request.createdAt,
  }));

  return (
    <ScreenLayout variant="detail">
      <BackLink href="/groom/horse-edit-requests">Change requests</BackLink>

      <Panel padded className="space-y-2">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h1 className="text-xl font-bold text-[var(--color-text-primary)]">{request.horseName ?? `Horse #${request.horseId}`}</h1>
            <p className="text-sm text-[var(--color-text-muted)]">
              Requested by {request.ownerName ?? `owner #${request.ownerId}`} on {formatDateTime(request.createdAt)}
              {request.horseRegistrationNumber ? ` · UELN ${request.horseRegistrationNumber}` : ''}
            </p>
          </div>
          <Pill tone={EDIT_STATUS_TONE[request.status]}>{EDIT_STATUS_LABEL[request.status]}</Pill>
        </div>
        {!pending && request.reviewedAt && (
          <p className="text-sm text-[var(--color-text-secondary)]">
            {EDIT_STATUS_LABEL[request.status]} by {request.reviewerName ?? 'groom'} on {formatDateTime(request.reviewedAt)}
            {request.reviewFeedback ? ` — “${request.reviewFeedback}”` : ''}
          </p>
        )}
      </Panel>

      <Panel padded className="space-y-3">
        <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Details</h2>
        {changes.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">No detail changes; only documents.</p>
        ) : (
          <Table bare>
            <THead>
              <Tr><Th>Field</Th><Th>Current</Th><Th>Requested</Th></Tr>
            </THead>
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
      </Panel>

      {request.documents.length > 0 && (
        <Panel padded className="space-y-4">
          <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Documents</h2>
          <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
            <section className="space-y-2">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Current</h3>
              <DocumentSlotGrid slots={currentSlots.filter((slot) =>
                request.documents.some((doc) => doc.documentType === slot.documentType))} />
            </section>
            <section className="space-y-2">
              <h3 className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-muted)]">Requested</h3>
              <DocumentSlotGrid slots={requestedSlots} />
            </section>
          </div>
          <p className="text-xs text-[var(--color-text-muted)]">Only the document types listed are part of this request.</p>
        </Panel>
      )}

      {pending && (
        <Panel padded className="space-y-3">
          <Notice tone="info">Approving applies these changes to the horse immediately and replaces the listed documents.</Notice>
          <div className="flex flex-wrap gap-2">
            <Button variant="primary" disabled={submitting} onClick={() => setDecision('APPROVED')}>Approve</Button>
            <Button variant="destructive" disabled={submitting} onClick={() => setDecision('REJECTED')}>Reject</Button>
          </div>
        </Panel>
      )}

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
    </ScreenLayout>
  );
}
