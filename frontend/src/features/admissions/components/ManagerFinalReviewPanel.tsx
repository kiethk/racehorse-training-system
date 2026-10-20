'use client';

import { useState } from 'react';
import { ApiError } from '@/services/api';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse } from '../types';
import { Button, Checkbox, ConfirmDialog, FormField, Notice, Select, Textarea } from '@/components/ui';

interface Props {
  detailData: AdmissionDetailResponse;
  onSuccess: () => void;
}

export function ManagerFinalReviewPanel({ detailData, onSuccess }: Props) {
  const [actionMode, setActionMode] = useState<'INITIAL' | 'APPROVE' | 'REJECT'>('INITIAL');
  const [stallMode, setStallMode] = useState<'auto' | 'manual'>('auto');
  const [stallId, setStallId] = useState<number | null>(null);
  const [feedback, setFeedback] = useState('');
  
  const [confirmDecision, setConfirmDecision] = useState<'APPROVED' | 'REJECTED' | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const availableStalls = detailData.availableRegularStalls || [];

  const handleActionClick = (decision: 'APPROVED' | 'REJECTED') => {
    setError(null);
    if (decision === 'REJECTED' && !feedback.trim()) {
      setError('Feedback is required to reject an application.');
      return;
    }
    if (decision === 'APPROVED' && stallMode === 'manual' && !stallId) {
      setError('Please select a stall or switch to automatic assignment.');
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
        stallId: confirmDecision === 'APPROVED' && stallMode === 'manual' ? stallId : null,
      });
      setConfirmDecision(null);
      setFeedback('');
      setStallId(null);
      setStallMode('auto');
      setActionMode('INITIAL');
      onSuccess();
    } catch (err: unknown) {
      const msg = err instanceof ApiError
        ? err.message
        : err instanceof Error
          ? err.message
          : 'Failed to submit review';
      setError(msg);
      setConfirmDecision(null);
    } finally {
      setSubmitting(false);
    }
  };
  
  const cancelAction = () => {
    setActionMode('INITIAL');
    setStallMode('auto');
    setStallId(null);
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
            <fieldset className="space-y-2">
              <legend className="text-xs font-medium text-[var(--color-text-primary)]">Stall assignment</legend>
              <Checkbox
                type="radio"
                name="stallMode"
                label="Automatic assignment"
                checked={stallMode === 'auto'}
                onChange={() => setStallMode('auto')}
              />
              <Checkbox
                type="radio"
                name="stallMode"
                label="Manual selection"
                checked={stallMode === 'manual'}
                onChange={() => setStallMode('manual')}
                disabled={availableStalls.length === 0}
              />
            </fieldset>

            {stallMode === 'manual' && (
              availableStalls.length > 0 ? (
                <FormField label="Regular stall" required>
                  <Select value={stallId || ''} onChange={(e) => setStallId(Number(e.target.value))}>
                    <option value="" disabled>Select a stall...</option>
                    {availableStalls.map(stall => (
                      <option key={stall.id} value={stall.id}>Stall {stall.stallCode}</option>
                    ))}
                  </Select>
                </FormField>
              ) : (
                <Notice tone="error">No regular stalls available. Cannot approve in manual mode.</Notice>
              )
            )}

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
              <Button variant="primary" onClick={() => handleActionClick('APPROVED')} disabled={submitting || (stallMode === 'manual' && (!stallId || availableStalls.length === 0))}>
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
        description="The candidate will become ELIGIBLE, the quarantine stall will be released, and the regular stall will be occupied."
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
