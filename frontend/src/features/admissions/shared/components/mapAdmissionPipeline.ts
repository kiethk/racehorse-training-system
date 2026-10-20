import type { IconName } from '@/components/ui/Icon';
import type { AdmissionStatus, TrainingDecision } from '../../types';

export type StepState = 'completed' | 'current' | 'pending' | 'rejected' | 'warning';

export interface AdmissionPipelineStep {
  id: string;
  label: string;
  icon: IconName;
  state: StepState;
  timestamp: string | null;
  note?: string;
}

export interface PipelineDetailInput {
  status: AdmissionStatus;
  submittedAt?: string | null;
  groomReviewedAt?: string | null;
  groomDecision?: string | null;
  quarantineStallCode?: string | null;
  arrivalDeadlineAt?: string | null;
  arrivedAt?: string | null;
  vetReviewedAt?: string | null;
  vetTrainingDecision?: TrainingDecision | null;
  trainerReviewedAt?: string | null;
  managerReviewedAt?: string | null;
  managerDecision?: string | null;
}

export interface PipelineMappingResult {
  steps: AdmissionPipelineStep[];
  isRejected: boolean;
  isApproved: boolean;
  isArrivalExpired: boolean;
  rejectionMessage: string | null;
}

/**
 * Pure mapper converting backend admission detail into 6 discrete pipeline steps:
 *  0. Submitted
 *  1. Groom Review
 *  2. Arrival (handles WAITING_FOR_STALL as current with "Waiting for stall" note)
 *  3. Vet Review
 *  4. Trainer Review
 *  5. Manager Review
 */
export function mapAdmissionPipeline(detail: PipelineDetailInput): PipelineMappingResult {
  const status = detail.status;
  const isRejected = status === 'REJECTED';
  const isApproved = status === 'APPROVED';
  const isArrivalExpired = status === 'ARRIVAL_EXPIRED';

  // 1. Determine which actor caused the rejection if status is REJECTED
  let rejectedStepIndex: number | null = null;
  let rejectionMessage: string | null = null;

  if (isRejected) {
    if (detail.managerDecision === 'REJECTED') {
      rejectedStepIndex = 5; // Manager Review is index 5
      rejectionMessage = 'Application was rejected during Manager Review.';
    } else if (detail.groomDecision === 'REJECTED') {
      rejectedStepIndex = 1; // Groom Review is index 1
      rejectionMessage = 'Application was rejected during Groom Review.';
    } else {
      rejectionMessage = 'This application has been rejected.';
    }
  }

  // 2. Normal sequential progression thresholds (0 to 5)
  // Indices:
  // 0: Submitted
  // 1: Groom Review
  // 2: Arrival (includes WAITING_FOR_STALL and WAITING_FOR_ARRIVAL)
  // 3: Vet Review
  // 4: Trainer Review
  // 5: Manager Review
  const statusOrder: Record<AdmissionStatus, number> = {
    GROOM_REVIEW: 1,
    WAITING_FOR_STALL: 2,
    WAITING_FOR_ARRIVAL: 2,
    ARRIVAL_EXPIRED: 2,
    VET_REVIEW: 3,
    TRAINER_REVIEW: 4,
    MANAGER_REVIEW: 5,
    APPROVED: 6, // past step 5
    REJECTED: -1,
  };

  const statusProgress = statusOrder[status] ?? 0;

  // Step 0: Submitted
  // Always completed once created.
  const step0State: StepState = 'completed';

  // Step 1: Groom Review
  let step1State: StepState = 'pending';
  if (rejectedStepIndex === 1) {
    step1State = 'rejected';
  } else if (detail.groomReviewedAt || statusProgress > 1) {
    step1State = 'completed';
  } else if (status === 'GROOM_REVIEW') {
    step1State = 'current';
  }

  // Step 2: Arrival
  // Handles:
  // - WAITING_FOR_STALL: current, note "Waiting for stall"
  // - WAITING_FOR_ARRIVAL: current, note "Stall <code...>"
  // - ARRIVAL_EXPIRED: warning, note "Arrival expired"
  // - Past arrived: completed
  let step2State: StepState = 'pending';
  let step2Note: string | undefined;

  if (isArrivalExpired) {
    step2State = 'warning';
    step2Note = 'Arrival expired';
  } else if (rejectedStepIndex !== null && rejectedStepIndex <= 1) {
    step2State = 'pending';
  } else if (detail.arrivedAt || statusProgress > 2) {
    step2State = 'completed';
  } else if (status === 'WAITING_FOR_STALL') {
    step2State = 'current';
    step2Note = 'Waiting for stall';
  } else if (status === 'WAITING_FOR_ARRIVAL') {
    step2State = 'current';
    if (detail.quarantineStallCode) {
      step2Note = `Stall ${detail.quarantineStallCode}`;
    }
  }

  // Step 3: Vet Review
  let step3State: StepState = 'pending';
  let step3Note: string | undefined;

  if (rejectedStepIndex !== null && rejectedStepIndex <= 2) {
    step3State = 'pending';
  } else if (detail.vetReviewedAt || statusProgress > 3) {
    step3State = 'completed';
    if (detail.vetTrainingDecision === 'BLOCKED') {
      step3Note = 'Training blocked';
    }
  } else if (status === 'VET_REVIEW') {
    step3State = 'current';
  }

  // Step 4: Trainer Review
  let step4State: StepState = 'pending';
  if (rejectedStepIndex !== null && rejectedStepIndex <= 3) {
    step4State = 'pending';
  } else if (detail.trainerReviewedAt || statusProgress > 4) {
    step4State = 'completed';
  } else if (status === 'TRAINER_REVIEW') {
    step4State = 'current';
  }

  // Step 5: Manager Review
  let step5State: StepState = 'pending';
  if (rejectedStepIndex === 5) {
    step5State = 'rejected';
  } else if (isApproved || detail.managerReviewedAt || statusProgress > 5) {
    step5State = 'completed';
  } else if (status === 'MANAGER_REVIEW') {
    step5State = 'current';
  }

  const steps: AdmissionPipelineStep[] = [
    {
      id: 'submitted',
      label: 'Submitted',
      icon: 'file-text',
      state: step0State,
      timestamp: detail.submittedAt ?? null,
    },
    {
      id: 'groom-review',
      label: 'Groom Review',
      icon: 'clipboard',
      state: step1State,
      timestamp: detail.groomReviewedAt ?? null,
    },
    {
      id: 'arrival',
      label: 'Arrival',
      icon: 'horse',
      state: step2State,
      timestamp: isArrivalExpired ? null : (detail.arrivedAt ?? null),
      note: step2Note,
    },
    {
      id: 'vet-review',
      label: 'Vet Review',
      icon: 'stethoscope',
      state: step3State,
      timestamp: detail.vetReviewedAt ?? null,
      note: step3Note,
    },
    {
      id: 'trainer-review',
      label: 'Trainer Review',
      icon: 'target',
      state: step4State,
      timestamp: detail.trainerReviewedAt ?? null,
    },
    {
      id: 'manager-review',
      label: 'Manager Review',
      icon: 'check',
      state: step5State,
      timestamp: detail.managerReviewedAt ?? null,
    },
  ];

  return {
    steps,
    isRejected,
    isApproved,
    isArrivalExpired,
    rejectionMessage,
  };
}
