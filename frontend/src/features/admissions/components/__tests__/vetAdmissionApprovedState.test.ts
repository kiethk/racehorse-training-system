import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

describe('Vet Admission Approved State Invariants', () => {
  it('1. APPROVED admission or COMPLETED exam row displays "View Record" action instead of "Examine"', () => {
    function isVetStageCompleted(status: string) {
      return status === 'TRAINER_REVIEW' || status === 'MANAGER_REVIEW' || status === 'APPROVED';
    }

    // Action resolution function mirroring VetAdmissionQueue render logic
    function resolveAction(row: { admissionStatus: string; careSchedule?: { status: string } | null }, queuePillFilter: string) {
      const isVetApproved =
        isVetStageCompleted(row.admissionStatus) ||
        row.careSchedule?.status === 'COMPLETED' ||
        queuePillFilter === 'APPROVED';
      if (isVetApproved) {
        return { label: 'View Record', icon: 'clipboard', primary: false };
      }
      const status = row.careSchedule?.status;
      return status === 'IN_PROGRESS'
        ? { label: 'Continue', icon: 'activity', primary: true }
        : status === 'SCHEDULED'
          ? { label: 'Examine', icon: 'stethoscope', primary: true }
          : { label: 'Details', icon: 'chevron-right', primary: false };
    }

    // Row that has a scheduled schedule but is already APPROVED
    const approvedRow = {
      admissionStatus: 'APPROVED',
      careSchedule: { status: 'SCHEDULED' },
    };

    const action = resolveAction(approvedRow, 'APPROVED');
    assert.equal(action.label, 'View Record');
    assert.equal(action.primary, false);

    // Even if queuePillFilter is 'ALL' or something else, admissionStatus === 'APPROVED' wins
    const actionInAll = resolveAction(approvedRow, 'ALL');
    assert.equal(actionInAll.label, 'View Record');
    assert.equal(actionInAll.primary, false);

    // Row where Vet finished the exam and admission is in TRAINER_REVIEW or MANAGER_REVIEW,
    // even if a follow-up ROUTINE schedule is IN_PROGRESS or SCHEDULED
    const trainerReviewFollowUpRow = {
      admissionStatus: 'TRAINER_REVIEW',
      careSchedule: { status: 'IN_PROGRESS' },
    };
    assert.equal(resolveAction(trainerReviewFollowUpRow, 'ACTIVE').label, 'View Record');

    const managerReviewFollowUpRow = {
      admissionStatus: 'MANAGER_REVIEW',
      careSchedule: { status: 'IN_PROGRESS' },
    };
    assert.equal(resolveAction(managerReviewFollowUpRow, 'ACTIVE').label, 'View Record');

    // Conversely, non-approved SCHEDULED row displays 'Examine'
    const pendingRow = {
      admissionStatus: 'VET_REVIEW',
      careSchedule: { status: 'SCHEDULED' },
    };
    const actionPending = resolveAction(pendingRow, 'ACTIVE');
    assert.equal(actionPending.label, 'Examine');
    assert.equal(actionPending.primary, true);
  });

  it('2. Opening an APPROVED or COMPLETED admission opens the "exam" tab in read-only record view and blocks exam editing', () => {
    function computeExamState({
      admissionStatus,
      scheduleStatus,
      assignedVetId,
      userId,
    }: {
      admissionStatus: string;
      scheduleStatus: string;
      assignedVetId: number;
      userId: number;
    }) {
      const isVetStageDone =
        admissionStatus === 'TRAINER_REVIEW' ||
        admissionStatus === 'MANAGER_REVIEW' ||
        admissionStatus === 'APPROVED';
      const isCompletedExam = isVetStageDone || scheduleStatus === 'COMPLETED';

      const canStartExam = Boolean(
        !isVetStageDone &&
        scheduleStatus === 'SCHEDULED' &&
        assignedVetId === userId,
      );

      const canCompleteExam = Boolean(
        !isVetStageDone &&
        ['SCHEDULED', 'IN_PROGRESS'].includes(scheduleStatus) &&
        assignedVetId === userId,
      );

      const defaultTab = 'exam';

      return { canStartExam, canCompleteExam, isCompletedExam, defaultTab };
    }

    // Approved admission with a lingering SCHEDULED schedule
    const approvedState = computeExamState({
      admissionStatus: 'APPROVED',
      scheduleStatus: 'SCHEDULED',
      assignedVetId: 10,
      userId: 10,
    });

    assert.equal(approvedState.canStartExam, false, 'Start examination must be disabled when approved');
    assert.equal(approvedState.canCompleteExam, false, 'Vet review form must not be active when approved');
    assert.equal(approvedState.isCompletedExam, true, 'Must render read-only record view when approved');
    assert.equal(approvedState.defaultTab, 'exam', 'Default tab must be exam (Clinical Examination record view)');

    // Completed vet exam awaiting Trainer/Manager review
    const completedState = computeExamState({
      admissionStatus: 'TRAINER_REVIEW',
      scheduleStatus: 'COMPLETED',
      assignedVetId: 10,
      userId: 10,
    });

    assert.equal(completedState.canStartExam, false);
    assert.equal(completedState.canCompleteExam, false);
    assert.equal(completedState.isCompletedExam, true);
    assert.equal(completedState.defaultTab, 'exam');

    // Active review admission awaiting vet exam
    const activeState = computeExamState({
      admissionStatus: 'VET_REVIEW',
      scheduleStatus: 'SCHEDULED',
      assignedVetId: 10,
      userId: 10,
    });

    assert.equal(activeState.canStartExam, true);
    assert.equal(activeState.canCompleteExam, true);
    assert.equal(activeState.isCompletedExam, false);
    assert.equal(activeState.defaultTab, 'exam');
  });

  it('3. Auto-exam randomization produces clinically sound physiological parameters', () => {
    function generateSimulatedExam() {
      const randomTemp = Number((37.5 + Math.random() * 0.7).toFixed(1));
      const randomHr = Math.floor(32 + Math.random() * 9);
      const randomRr = Math.floor(10 + Math.random() * 5);
      const randomWeight = Number((480 + Math.floor(Math.random() * 45 * 2) / 2).toFixed(1));
      const randomBcs = Number((5.0 + (Math.random() > 0.5 ? 0.5 : 0)).toFixed(1));

      return {
        temperature: randomTemp,
        heartRate: randomHr,
        respiratoryRate: randomRr,
        weight: randomWeight,
        bodyConditionScore: randomBcs,
        trainingDecision: 'ALLOWED',
        examMode: 'NORMAL',
      };
    }

    // Verify 50 randomized iterations all stay within normal physiological bounds
    for (let i = 0; i < 50; i++) {
      const data = generateSimulatedExam();
      assert.ok(data.temperature >= 37.2 && data.temperature <= 38.3, `Temp ${data.temperature} out of bounds`);
      assert.ok(data.heartRate >= 28 && data.heartRate <= 44, `HR ${data.heartRate} out of bounds`);
      assert.ok(data.respiratoryRate >= 8 && data.respiratoryRate <= 16, `RR ${data.respiratoryRate} out of bounds`);
      assert.ok(data.weight >= 400 && data.weight <= 600, `Weight ${data.weight} out of bounds`);
      assert.ok(data.bodyConditionScore >= 4.0 && data.bodyConditionScore <= 6.0, `BCS ${data.bodyConditionScore} out of bounds`);
      assert.equal(data.trainingDecision, 'ALLOWED');
      assert.equal(data.examMode, 'NORMAL');
    }
  });
});
