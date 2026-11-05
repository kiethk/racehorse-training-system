export type ReviewDecisionValue = 'APPROVED' | 'REJECTED';

/** Mirrors the server limit for review feedback. */
export const REVIEW_FEEDBACK_MAX = 2000;

/**
 * Approve may go without feedback; reject needs a reason. The server enforces the
 * same rule, this only keeps the confirm button honest.
 */
export function canConfirmDecision(decision: ReviewDecisionValue, feedback: string): boolean {
  const trimmed = feedback.trim();
  if (trimmed.length > REVIEW_FEEDBACK_MAX) return false;
  return decision === 'APPROVED' || trimmed.length > 0;
}

/** Trimmed feedback, or undefined when blank so an empty approval sends no feedback at all. */
export function normalizeFeedback(feedback: string): string | undefined {
  const trimmed = feedback.trim();
  return trimmed ? trimmed : undefined;
}
