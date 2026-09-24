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
  const [mode, setMode] = useState<'auto' | 'manual'>('auto');
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
    if (decision === 'APPROVED' && mode === 'manual' && !stallId) {
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
        stallId: confirmDecision === 'APPROVED' && mode === 'manual' ? stallId : null,
      });
      setConfirmDecision(null);
      setFeedback('');
      setStallId(null);
      setMode('auto');
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

          <div className="space-y-3">
            <label className="text-[13px] font-medium text-[var(--color-text-primary)]">Stall Assignment</label>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 text-[13px] cursor-pointer">
                <input 
                  type="radio" 
                  name="stallMode" 
                  checked={mode === 'auto'} 
                  onChange={() => setMode('auto')} 
                  className="accent-[var(--color-primary)]"
                />
                Automatic (First available)
              </label>
              <label className="flex items-center gap-2 text-[13px] cursor-pointer">
                <input 
                  type="radio" 
                  name="stallMode" 
                  checked={mode === 'manual'} 
                  onChange={() => setMode('manual')} 
                  className="accent-[var(--color-primary)]"
                  disabled={availableStalls.length === 0}
                />
                Manual selection
              </label>
            </div>
            
            {mode === 'manual' && (
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
                  <div className="text-[12px] text-[var(--color-danger)] flex items-center gap-1.5">
                    <Icon name="alert-triangle" size={14} />
                    No regular stalls available. Cannot approve in manual mode.
                  </div>
                )}
              </div>
            )}
          </div>

          <div className="space-y-1">
            <label className="text-[13px] font-medium text-[var(--color-text-primary)]">
              Feedback {confirmDecision === 'REJECTED' && <span className="text-[var(--color-danger)]">*</span>}
            </label>
            <textarea
              className="w-full p-2 text-[13px] border border-[var(--color-border)] rounded bg-[var(--color-surface)] outline-none focus:border-[var(--color-primary)] resize-none"
              rows={3}
              placeholder="Enter feedback or reasoning (required for rejection)..."
              value={feedback}
              onChange={(e) => setFeedback(e.target.value)}
            />
          </div>

          <div className="flex gap-2 justify-end pt-2">
            <Button variant="destructive" onClick={() => handleActionClick('REJECTED')} disabled={submitting}>
              Reject
            </Button>
            <Button variant="primary" onClick={() => handleActionClick('APPROVED')} disabled={submitting || (mode === 'manual' && availableStalls.length === 0)}>
              Approve
            </Button>
          </div>
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
