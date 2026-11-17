import test from 'node:test';
import assert from 'node:assert/strict';
import { canApproveAdmission } from '../../shared/lib/capacity.ts';

test('canApproveAdmission returns true when the reserve is sufficient', () => {
  assert.equal(canApproveAdmission({ approvalCapacityAvailable: true }), true);
});

test('canApproveAdmission returns false when the quarantine pipeline is over capacity', () => {
  assert.equal(canApproveAdmission({ approvalCapacityAvailable: false }), false);
});

test('canApproveAdmission keeps legacy payloads enabled so the backend can decide', () => {
  assert.equal(canApproveAdmission(undefined), true);
  assert.equal(canApproveAdmission(null), true);
});
