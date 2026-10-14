import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { mapAdmissionPipeline } from '../../shared/components/mapAdmissionPipeline.ts';
import type { TrainingDecision } from '../../types/index.ts';

describe('AdmissionPipeline Mapper Unit Tests (6 Steps)', () => {
  it('1. GROOM_REVIEW: Submitted is completed, Groom Review is current, subsequent steps are pending', () => {
    const res = mapAdmissionPipeline({
      status: 'GROOM_REVIEW',
      submittedAt: '2026-10-01T08:00:00Z',
    });

    assert.equal(res.isRejected, false);
    assert.equal(res.isApproved, false);
    assert.equal(res.steps.length, 6);

    assert.equal(res.steps[0].id, 'submitted');
    assert.equal(res.steps[0].state, 'completed');
    assert.equal(res.steps[0].timestamp, '2026-10-01T08:00:00Z');

    assert.equal(res.steps[1].id, 'groom-review');
    assert.equal(res.steps[1].state, 'current');

    for (let i = 2; i < 6; i++) {
      assert.equal(res.steps[i].state, 'pending');
    }
  });

  it('2. WAITING_FOR_STALL: Submitted & Groom Review are completed, Arrival is current with "Waiting for stall" note', () => {
    const res = mapAdmissionPipeline({
      status: 'WAITING_FOR_STALL',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      groomDecision: 'APPROVED',
    });

    assert.equal(res.steps.length, 6);
    assert.equal(res.steps[0].state, 'completed');
    assert.equal(res.steps[1].state, 'completed');
    assert.equal(res.steps[1].timestamp, '2026-10-01T09:00:00Z');

    // Step 2 is Arrival
    assert.equal(res.steps[2].id, 'arrival');
    assert.equal(res.steps[2].state, 'current');
    assert.equal(res.steps[2].note, 'Waiting for stall');
    assert.equal(res.steps[2].timestamp, null); // Not arrived yet

    assert.equal(res.steps[3].id, 'vet-review');
    assert.equal(res.steps[3].state, 'pending');
  });

  it('3. WAITING_FOR_ARRIVAL: Arrival is current with reserved stall note', () => {
    const res = mapAdmissionPipeline({
      status: 'WAITING_FOR_ARRIVAL',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      quarantineStallCode: 'Q-01',
      arrivalDeadlineAt: '2026-10-15T00:00:00Z',
    });

    assert.equal(res.steps[0].state, 'completed');
    assert.equal(res.steps[1].state, 'completed');

    assert.equal(res.steps[2].id, 'arrival');
    assert.equal(res.steps[2].state, 'current');
    assert.equal(res.steps[2].note, 'Stall Q-01');
    assert.equal(res.steps[2].timestamp, null);

    assert.equal(res.steps[3].state, 'pending');
  });

  it('4. ARRIVAL_EXPIRED: Arrival is warning/expired with note', () => {
    const res = mapAdmissionPipeline({
      status: 'ARRIVAL_EXPIRED',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      quarantineStallCode: 'Q-01',
    });

    assert.equal(res.isArrivalExpired, true);
    assert.equal(res.steps[2].id, 'arrival');
    assert.equal(res.steps[2].state, 'warning');
    assert.equal(res.steps[2].note, 'Arrival expired');
    assert.equal(res.steps[2].timestamp, null); // never arrived
  });

  it('5. VET_REVIEW: Steps 0-2 completed, Vet Review is current', () => {
    const res = mapAdmissionPipeline({
      status: 'VET_REVIEW',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      quarantineStallCode: 'Q-01',
      arrivedAt: '2026-10-03T14:00:00Z',
    });

    assert.equal(res.steps[2].id, 'arrival');
    assert.equal(res.steps[2].state, 'completed');
    assert.equal(res.steps[2].timestamp, '2026-10-03T14:00:00Z');

    assert.equal(res.steps[3].id, 'vet-review');
    assert.equal(res.steps[3].state, 'current');
  });

  it('6. TRAINER_REVIEW with Vet BLOCKED: Vet Review is completed with "Training blocked" note, does not reject admission', () => {
    const res = mapAdmissionPipeline({
      status: 'TRAINER_REVIEW',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      arrivedAt: '2026-10-03T14:00:00Z',
      vetReviewedAt: '2026-10-04T10:00:00Z',
      vetTrainingDecision: 'BLOCKED' as TrainingDecision,
    });

    assert.equal(res.isRejected, false);
    assert.equal(res.steps[3].id, 'vet-review');
    assert.equal(res.steps[3].state, 'completed');
    assert.equal(res.steps[3].note, 'Training blocked');
    assert.equal(res.steps[3].timestamp, '2026-10-04T10:00:00Z');

    assert.equal(res.steps[4].id, 'trainer-review');
    assert.equal(res.steps[4].state, 'current');
  });

  it('7. MANAGER_REVIEW: Steps 0-4 completed, Manager Review is current', () => {
    const res = mapAdmissionPipeline({
      status: 'MANAGER_REVIEW',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      arrivedAt: '2026-10-03T14:00:00Z',
      vetReviewedAt: '2026-10-04T10:00:00Z',
      trainerReviewedAt: '2026-10-05T15:00:00Z',
    });

    assert.equal(res.steps[4].id, 'trainer-review');
    assert.equal(res.steps[4].state, 'completed');
    assert.equal(res.steps[4].timestamp, '2026-10-05T15:00:00Z');

    assert.equal(res.steps[5].id, 'manager-review');
    assert.equal(res.steps[5].state, 'current');
  });

  it('8. APPROVED: All 6 steps completed, isApproved is true', () => {
    const res = mapAdmissionPipeline({
      status: 'APPROVED',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      arrivedAt: '2026-10-03T14:00:00Z',
      vetReviewedAt: '2026-10-04T10:00:00Z',
      trainerReviewedAt: '2026-10-05T15:00:00Z',
      managerReviewedAt: '2026-10-06T16:00:00Z',
      managerDecision: 'APPROVED',
    });

    assert.equal(res.isApproved, true);
    assert.equal(res.steps.length, 6);
    for (let i = 0; i < 6; i++) {
      assert.equal(res.steps[i].state, 'completed');
    }
    assert.equal(res.steps[5].timestamp, '2026-10-06T16:00:00Z');
  });

  it('9. REJECTED by Groom: Groom Review marked rejected, message cites Groom Review, later steps pending', () => {
    const res = mapAdmissionPipeline({
      status: 'REJECTED',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      groomDecision: 'REJECTED',
    });

    assert.equal(res.isRejected, true);
    assert.match(res.rejectionMessage ?? '', /Groom Review/);
    assert.equal(res.steps[0].state, 'completed');
    assert.equal(res.steps[1].state, 'rejected');
    for (let i = 2; i < 6; i++) {
      assert.equal(res.steps[i].state, 'pending');
    }
  });

  it('10. REJECTED by Manager: Manager Review marked rejected, message cites Manager Review', () => {
    const res = mapAdmissionPipeline({
      status: 'REJECTED',
      submittedAt: '2026-10-01T08:00:00Z',
      groomReviewedAt: '2026-10-01T09:00:00Z',
      groomDecision: 'APPROVED',
      arrivedAt: '2026-10-03T14:00:00Z',
      vetReviewedAt: '2026-10-04T10:00:00Z',
      trainerReviewedAt: '2026-10-05T15:00:00Z',
      managerReviewedAt: '2026-10-06T16:00:00Z',
      managerDecision: 'REJECTED',
    });

    assert.equal(res.isRejected, true);
    assert.match(res.rejectionMessage ?? '', /Manager Review/);
    for (let i = 0; i < 5; i++) {
      assert.equal(res.steps[i].state, 'completed');
    }
    assert.equal(res.steps[5].id, 'manager-review');
    assert.equal(res.steps[5].state, 'rejected');
  });

  it('11. REJECTED with missing decision data: gives generic message, does not speculate on actor', () => {
    const res = mapAdmissionPipeline({
      status: 'REJECTED',
      submittedAt: '2026-10-01T08:00:00Z',
    });

    assert.equal(res.isRejected, true);
    assert.equal(res.rejectionMessage, 'This application has been rejected.');
  });

  it('12. Owner perspective with omitted timestamps: derives status-proven completed stages, no fake dates', () => {
    const res = mapAdmissionPipeline({
      status: 'TRAINER_REVIEW',
      // Owner response omits all reviewer timestamps
    });

    assert.equal(res.steps.length, 6);
    assert.equal(res.steps[0].state, 'completed');
    assert.equal(res.steps[0].timestamp, null); // no fake timestamp fabricated
    assert.equal(res.steps[1].state, 'completed');
    assert.equal(res.steps[1].timestamp, null);
    assert.equal(res.steps[2].state, 'completed');
    assert.equal(res.steps[2].timestamp, null);
    assert.equal(res.steps[3].state, 'completed');
    assert.equal(res.steps[3].timestamp, null);
    assert.equal(res.steps[4].state, 'current');
    assert.equal(res.steps[5].state, 'pending');
  });
});
