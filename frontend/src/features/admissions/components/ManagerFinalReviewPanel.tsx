'use client';

import { useState } from 'react';
import { ApiError } from '@/services/api';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse } from '../types';
import { Button, ConfirmDialog, FormField, Notice, Textarea } from '@/components/ui';
import { toast } from '@/lib/toast';
import { canApproveAdmission } from '../shared/lib/capacity';

interface Props {
  detailData: AdmissionDetailResponse;
  onSuccess: () => void;
}

export function ManagerFinalReviewPanel({ detailData, onSuccess }: Props) {
  const [actionMode, setActionMode] = useState<'INITIAL' | 'APPROVE' | 'REJECT'>('INITIAL');
  const [feedback, setFeedback] = useState('');
  
  const [confirmDecision, setConfirmDecision] = useState<'APPROVED' | 'REJECTED' | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const capacity = detailData.capacity;
  const approvalCapacityAvailable = canApproveAdmission(capacity);

  const handleActionClick = (decision: 'APPROVED' | 'REJECTED') => {
    setError(null);
    if (decision === 'REJECTED' && !feedback.trim()) {
      setError('Feedback is required to reject an application.');
      return;
    }
    setConfirmDecision(decision);
  };

  const handleConfirm = async () => {
    try {
      setSubmitting(true);
      setError(null);
      await admissionsApi.managerReview(detailData.admissionId, {
        decision: confirmDecision!,
        feedback: feedback.trim() || undefined,
      });
      setConfirmDecision(null);
      setFeedback('');
      setActionMode('INITIAL');
      toast.success(confirmDecision === 'APPROVED' ? 'Admission approved' : 'Admission rejected',
        confirmDecision === 'APPROVED'
          ? 'The horse is now eligible and has moved to its regular stall.'
          : 'The owner can see your reason on the application.');
      onSuccess();
    } catch (err: unknown) {
      const msg = err instanceof ApiError
        ? err.message
        : err instanceof Error
          ? err.message
          : 'Failed to submit review';
      setError(msg);
      toast.error('Review not saved', msg);
      setConfirmDecision(null);
      if (err instanceof ApiError && err.errorCode === 'REGULAR_RESERVE_INSUFFICIENT') {
        onSuccess();
      }
    } finally {
      setSubmitting(false);
    }
  };
  
  const cancelAction = () => {
    setActionMode('INITIAL');
    setFeedback('');
    setError(null);
  };

  if (detailData.status !== 'MANAGER_REVIEW') {
    return null;
  }

  return (
    <>
      <div className="space-y-4">
        {error && <Notice tone="error">{error}</Notice>}

        {!approvalCapacityAvailable && capacity && (
          <Notice tone="warning">
            Cannot approve yet: {capacity.occupiedQuarantineStalls} horse(s) are in quarantine and need{' '}
            {capacity.requiredRegularStallsForApproval} free regular stall(s), but only{' '}
            {capacity.availableRegularStalls} are available. Free up a regular stall first.
          </Notice>
        )}

        {actionMode === 'INITIAL' && (
          <div className="grid grid-cols-2 gap-2">
            <Button variant="destructive" onClick={() => setActionMode('REJECT')}>
              Reject
            </Button>
            <Button variant="primary" onClick={() => setActionMode('APPROVE')}>
              Approve
            </Button>
          </div>
        )}

        {actionMode === 'APPROVE' && (
          <div className="space-y-4">
            <div className="p-3 rounded border border-[var(--color-border)] bg-[var(--color-surface-hover)]/40 text-xs space-y-1">
              <div className="font-semibold text-[var(--color-text-primary)]">Stall assignment: Automatic</div>
              <p className="text-[var(--color-text-muted)]">
                The horse will be automatically assigned to the fairest available regular stall with an active groom.
              </p>
            </div>

            <FormField label="Feedback (optional)">
              <Textarea
                rows={2}
                placeholder="Enter optional feedback..."
                value={feedback}
                onChange={(e) => setFeedback(e.target.value)}
              />
            </FormField>

            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={cancelAction} disabled={submitting}>
                Cancel
              </Button>
              <Button variant="primary" onClick={() => handleActionClick('APPROVED')} disabled={submitting || !approvalCapacityAvailable}>
                Confirm Approve
              </Button>
            </div>
          </div>
        )}

        {actionMode === 'REJECT' && (
          <div className="space-y-4">
            <FormField label="Feedback" required>
              <Textarea
                rows={3}
                placeholder="Enter reasoning (required for rejection)..."
                value={feedback}
                onChange={(e) => {
                  setFeedback(e.target.value);
                  if (error) setError(null);
                }}
              />
            </FormField>

            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={cancelAction} disabled={submitting}>
                Cancel
              </Button>
              <Button variant="destructive" onClick={() => handleActionClick('REJECTED')} disabled={submitting || !feedback.trim()}>
                Confirm Reject
              </Button>
            </div>
          </div>
        )}
      </div>

      <ConfirmDialog
        open={confirmDecision === 'APPROVED'}
        title="Approve Admission?"
        description="The candidate will become ELIGIBLE, the quarantine stall will be released, and a regular stall will be automatically assigned."
        confirmLabel="Approve"
        tone="primary"
        loading={submitting}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmDecision(null)}
      />

      <ConfirmDialog
        open={confirmDecision === 'REJECTED'}
        title="Reject Admission?"
        description="The admission and Horse will become REJECTED, the quarantine stall will be released, and pending/overdue preventive schedules will be cancelled."
        confirmLabel="Reject"
        tone="danger"
        loading={submitting}
        onConfirm={handleConfirm}
        onCancel={() => setConfirmDecision(null)}
      />
    </>
  );
}
