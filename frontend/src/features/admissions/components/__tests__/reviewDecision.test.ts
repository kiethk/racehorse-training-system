import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { canConfirmDecision, normalizeFeedback, REVIEW_FEEDBACK_MAX } from '../../shared/lib/reviewDecision.ts';

describe('Review decision feedback', () => {
  it('lets an approval go without feedback', () => {
    assert.equal(canConfirmDecision('APPROVED', ''), true);
    assert.equal(canConfirmDecision('APPROVED', '   '), true);
  });

  it('requires a non-blank reason to reject', () => {
    assert.equal(canConfirmDecision('REJECTED', ''), false);
    assert.equal(canConfirmDecision('REJECTED', ' \t\n '), false);
    assert.equal(canConfirmDecision('REJECTED', 'Missing vaccination record'), true);
  });

  it('blocks feedback over the server limit for both decisions', () => {
    const tooLong = 'x'.repeat(REVIEW_FEEDBACK_MAX + 1);
    assert.equal(canConfirmDecision('APPROVED', tooLong), false);
    assert.equal(canConfirmDecision('REJECTED', tooLong), false);
  });

  it('sends trimmed feedback, or nothing when blank', () => {
    assert.equal(normalizeFeedback('  Ready  '), 'Ready');
    assert.equal(normalizeFeedback('   '), undefined);
  });
});
