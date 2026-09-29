'use client';

import { useState } from 'react';
import { admissionsApi } from '../services/api';
import type { AdmissionDetailResponse } from '../types';
import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Button } from '@/components/ui/Button';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Icon } from '@/components/ui/Icon';

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
      if (err instanceof Error) {
        // @ts-expect-error - Handle Axios error shape gracefully
        setError(err.response?.data?.message || err.message || 'Failed to submit review');
      } else {
        setError('Failed to submit review');
      }
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
      <Panel padded className="border-[var(--color-primary)] border-2">
        <SectionTitle>Manager Final Review</SectionTitle>
        <div className="mt-4 space-y-4">
          
          {error && (
            <div className="p-3 bg-[var(--color-danger-soft)] text-[var(--color-danger)] text-[13px] rounded-[var(--radius-sm)] flex items-start gap-2">
              <Icon name="alert-triangle" className="shrink-0 mt-0.5" />
              <span>{error}</span>
            </div>
          )}

          {actionMode === 'INITIAL' && (
            <div className="flex gap-2">
              <Button variant="destructive" onClick={() => setActionMode('REJECT')}>
                Reject
              </Button>
              <Button variant="primary" onClick={() => setActionMode('APPROVE')}>
                Approve
              </Button>
            </div>
          )}
          
          {actionMode === 'APPROVE' && (
            <div className="space-y-4 animate-in fade-in slide-in-from-top-1">
              <div className="space-y-3">
                <label className="text-[13px] font-medium text-[var(--color-text-primary)]">Stall Assignment</label>
                <div className="flex gap-4">
                  <label className="flex items-center gap-2 text-[13px] cursor-pointer">
                    <input 
                      type="radio" 
                      name="stallMode" 
                      checked={stallMode === 'auto'} 
                      onChange={() => setStallMode('auto')} 
                      className="accent-[var(--color-primary)]"
                    />
                    Automatic assignment
                  </label>
                  <label className="flex items-center gap-2 text-[13px] cursor-pointer">
                    <input 
                      type="radio" 
                      name="stallMode" 
                      checked={stallMode === 'manual'} 
                      onChange={() => setStallMode('manual')} 
                      className="accent-[var(--color-primary)]"
                      disabled={availableStalls.length === 0}
                    />
                    Manual selection
                  </label>
                </div>
                
                {stallMode === 'manual' && (
                  <div className="mt-2">
                    {availableStalls.length > 0 ? (
                      <select 
                        value={stallId || ''} 
                        onChange={(e) => setStallId(Number(e.target.value))}
                        className="w-full max-w-sm p-2 text-[13px] border border-[var(--color-border)] rounded bg-[var(--color-surface)] outline-none focus:border-[var(--color-primary)]"
                      >
                        <option value="" disabled>Select a stall...</option>
                        {availableStalls.map(stall => (
                          <option key={stall.id} value={stall.id}>Stall {stall.stallCode}</option>
                        ))}
                      </select>
                    ) : (
                      <div className="text-[12px] text-[var(--color-danger)] flex items-center gap-1.5 p-2 bg-[var(--color-danger-soft)] rounded-[var(--radius-sm)]">
                        <Icon name="alert-triangle" size={14} />
                        No regular stalls available. Cannot approve in manual mode.
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="space-y-1">
                <label className="text-[13px] font-medium text-[var(--color-text-primary)]">
                  Feedback (Optional)
                </label>
                <textarea
                  className="w-full p-2 text-[13px] border border-[var(--color-border)] rounded bg-[var(--color-surface)] outline-none focus:border-[var(--color-primary)] resize-none"
                  rows={2}
                  placeholder="Enter optional feedback..."
                  value={feedback}
                  onChange={(e) => setFeedback(e.target.value)}
                />
              </div>

              <div className="flex gap-2 justify-end pt-2">
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
            <div className="space-y-4 animate-in fade-in slide-in-from-top-1">
              <div className="space-y-1">
                <label className="text-[13px] font-medium text-[var(--color-text-primary)]">
                  Feedback <span className="text-[var(--color-danger)]">*</span>
                </label>
                <textarea
                  className="w-full p-2 text-[13px] border border-[var(--color-border)] rounded bg-[var(--color-surface)] outline-none focus:border-[var(--color-primary)] resize-none"
                  rows={3}
                  placeholder="Enter reasoning (required for rejection)..."
                  value={feedback}
                  onChange={(e) => {
                    setFeedback(e.target.value);
                    if (error) setError(null);
                  }}
                />
              </div>

              <div className="flex gap-2 justify-end pt-2">
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
      </Panel>

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
