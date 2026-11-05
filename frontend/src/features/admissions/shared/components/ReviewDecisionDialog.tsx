'use client';

import { useEffect, useState } from 'react';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { FormField } from '@/components/ui/FormField';
import { Textarea } from '@/components/ui/Input';
import { Notice } from '@/components/ui/Notice';
import { canConfirmDecision, normalizeFeedback, REVIEW_FEEDBACK_MAX, type ReviewDecisionValue } from '../lib/reviewDecision';

interface ReviewDecisionDialogProps {
  /** Null keeps the dialog closed; a decision opens it for that decision. */
  decision: ReviewDecisionValue | null;
  title: string;
  description: string;
  loading?: boolean;
  /** Request error shown inside the dialog, so the typed feedback is not lost. */
  error?: string | null;
  onConfirm: (decision: ReviewDecisionValue, feedback: string | undefined) => void;
  onClose: () => void;
}

/**
 * Approve/Reject confirmation with feedback shown only once a decision is picked.
 * Approve: feedback optional. Reject: a reason is required (the server checks it too).
 * Closing or cancelling never submits.
 */
export function ReviewDecisionDialog({ decision, title, description, loading = false, error, onConfirm, onClose }: ReviewDecisionDialogProps) {
  const [feedback, setFeedback] = useState('');
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    // A new decision starts from an empty form.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setFeedback('');
    setTouched(false);
  }, [decision]);

  if (!decision) return null;

  const rejecting = decision === 'REJECTED';
  const valid = canConfirmDecision(decision, feedback);
  const fieldError = rejecting && touched && !feedback.trim() ? 'Enter a reason to reject.' : undefined;

  return (
    <ConfirmDialog
      open
      title={title}
      tone={rejecting ? 'danger' : 'primary'}
      confirmLabel={rejecting ? 'Reject' : 'Approve'}
      loading={loading}
      confirmDisabled={!valid}
      onCancel={onClose}
      onConfirm={() => {
        setTouched(true);
        if (valid) onConfirm(decision, normalizeFeedback(feedback));
      }}
      description={(
        <div className="space-y-4">
          <p>{description}</p>
          {error && <Notice tone="error">{error}</Notice>}
          <FormField
            label={rejecting ? 'Reason for rejection' : 'Feedback (optional)'}
            required={rejecting}
            error={fieldError}
            hint={`${feedback.length}/${REVIEW_FEEDBACK_MAX}`}
          >
            <Textarea
              autoFocus
              value={feedback}
              onChange={(event) => setFeedback(event.target.value)}
              onBlur={() => setTouched(true)}
              rows={4}
              maxLength={REVIEW_FEEDBACK_MAX}
            />
          </FormField>
        </div>
      )}
    />
  );
}
