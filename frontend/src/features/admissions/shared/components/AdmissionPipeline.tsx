'use client';

import { Panel, SectionTitle } from '@/components/ui/Panel';
import { Icon } from '@/components/ui/Icon';
import { formatDateTime } from '@/lib/display';
import { cn } from '@/lib/cn';
import type { AdmissionDetailResponse } from '../../types';
import { mapAdmissionPipeline, type StepState } from './mapAdmissionPipeline';

export type PipelineDetail = Pick<AdmissionDetailResponse, 'status'> &
  Partial<
    Pick<
      AdmissionDetailResponse,
      | 'submittedAt'
      | 'groomReviewedAt'
      | 'groomDecision'
      | 'quarantineStallCode'
      | 'arrivalDeadlineAt'
      | 'arrivedAt'
      | 'vetReviewedAt'
      | 'vetTrainingDecision'
      | 'trainerReviewedAt'
      | 'managerReviewedAt'
      | 'managerDecision'
    >
  >;

export function AdmissionPipeline({ detail }: { detail: PipelineDetail }) {
  const { steps, isRejected, isArrivalExpired, rejectionMessage } =
    mapAdmissionPipeline(detail);

  return (
    <Panel padded className="space-y-4 overflow-hidden">
      <div className="flex items-center justify-between gap-3">
        <SectionTitle>Review pipeline</SectionTitle>
      </div>

      {isRejected && (
        <div
          role="status"
          className="rounded-[var(--radius-sm)] border border-[var(--color-danger)] bg-[var(--color-danger-soft)] px-3 py-2 text-sm font-medium text-[var(--color-danger)]"
        >
          {rejectionMessage ?? 'This application has been rejected.'}
        </div>
      )}

      {isArrivalExpired && !isRejected && (
        <div
          role="status"
          className="rounded-[var(--radius-sm)] border border-[var(--color-warning)] bg-[var(--color-warning-soft)] px-3 py-2 text-sm font-medium text-[var(--color-warning)]"
        >
          The 14-day arrival window has expired. The reserved quarantine stall has been released.
        </div>
      )}

      {/* Shopee-style horizontal pipeline: 6 equidistant circles connected by center-running lines */}
      <div className="overflow-x-auto py-3">
        <div className="min-w-[640px]">
          <div className="grid grid-cols-6 items-start">
            {steps.map((step, idx) => {
              const isFirst = idx === 0;
              const isLast = idx === steps.length - 1;
              const prevStep = idx > 0 ? steps[idx - 1] : null;

              // Line to the left connects prevStep to current step
              // A segment is green/completed only if both previous and current steps have completed or current is completed
              const isLeftLineCompleted =
                prevStep?.state === 'completed' &&
                (step.state === 'completed' || step.state === 'current');

              // Line to the right connects current step to next step
              const nextStep = idx < steps.length - 1 ? steps[idx + 1] : null;
              const isRightLineCompleted =
                step.state === 'completed' &&
                (nextStep?.state === 'completed' || nextStep?.state === 'current');

              return (
                <div key={step.id} className="relative flex flex-col items-center text-center">
                  {/* Connecting line to the left */}
                  {!isFirst && (
                    <div
                      aria-hidden="true"
                      className={cn(
                        'absolute top-6 right-1/2 left-0 h-0.5 -translate-y-1/2',
                        isLeftLineCompleted
                          ? 'bg-[var(--color-success)]'
                          : 'bg-[var(--color-border)]',
                      )}
                    />
                  )}

                  {/* Connecting line to the right */}
                  {!isLast && (
                    <div
                      aria-hidden="true"
                      className={cn(
                        'absolute top-6 left-1/2 right-0 h-0.5 -translate-y-1/2',
                        isRightLineCompleted
                          ? 'bg-[var(--color-success)]'
                          : 'bg-[var(--color-border)]',
                      )}
                    />
                  )}

                  {/* Icon circle (48px / h-12 w-12) */}
                  <div className="relative z-10 flex h-12 w-12 items-center justify-center">
                    <StepCircle state={step.state} icon={step.icon} isFinal={isLast} />
                  </div>

                  {/* Step label, note, and real timestamp below icon */}
                  <div className="mt-2.5 w-full space-y-0.5 px-1">
                    <p
                      className={cn(
                        'text-xs font-semibold leading-tight',
                        step.state === 'completed' && 'text-[var(--color-success)]',
                        step.state === 'current' && 'text-[var(--color-primary)] font-bold',
                        step.state === 'rejected' && 'text-[var(--color-danger)] font-bold',
                        step.state === 'warning' && 'text-[var(--color-warning)] font-bold',
                        step.state === 'pending' && 'text-[var(--color-text-muted)]',
                      )}
                    >
                      {step.label}
                    </p>

                    {step.note && (
                      <p
                        className={cn(
                          'text-xs leading-tight',
                          step.state === 'warning'
                            ? 'text-[var(--color-warning)] font-medium'
                            : step.state === 'rejected'
                              ? 'text-[var(--color-danger)] font-medium'
                              : 'text-[var(--color-text-secondary)]',
                        )}
                      >
                        {step.note}
                      </p>
                    )}

                    {step.timestamp && (
                      <p className="text-xs text-[var(--color-text-muted)] leading-tight">
                        {formatDateTime(step.timestamp)}
                      </p>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </Panel>
  );
}

function StepCircle({
  state,
  icon,
  isFinal,
}: {
  state: StepState;
  icon: import('@/components/ui/Icon').IconName;
  isFinal: boolean;
}) {
  if (state === 'completed') {
    if (isFinal) {
      // Approved / final completed step: solid green background with white inverse icon
      return (
        <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-success)] text-[var(--color-text-inverse)] shadow-sm">
          <Icon name="check" size={20} />
        </div>
      );
    }
    // Completed intermediate step: green border, soft green background, green icon
    return (
      <div className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-[var(--color-success)] bg-[var(--color-surface)] text-[var(--color-success)] shadow-sm">
        <Icon name={icon} size={20} />
      </div>
    );
  }

  if (state === 'current') {
    // Current active step: primary background with inverse white icon
    return (
      <div className="flex h-11 w-11 items-center justify-center rounded-full bg-[var(--color-primary)] text-[var(--color-text-inverse)] shadow-md ring-4 ring-[var(--color-primary-soft)]">
        <Icon name={icon} size={20} />
      </div>
    );
  }

  if (state === 'rejected') {
    // Rejected step: danger red ring and icon
    return (
      <div className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-[var(--color-danger)] bg-[var(--color-danger-soft)] text-[var(--color-danger)] shadow-sm">
        <Icon name="x" size={20} />
      </div>
    );
  }

  if (state === 'warning') {
    // Expired / warning step: warning ring and icon
    return (
      <div className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-[var(--color-warning)] bg-[var(--color-warning-soft)] text-[var(--color-warning)] shadow-sm">
        <Icon name="alert-triangle" size={20} />
      </div>
    );
  }

  // Pending step: neutral gray border and muted icon
  return (
    <div className="flex h-11 w-11 items-center justify-center rounded-full border-2 border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)]">
      <Icon name={icon} size={20} />
    </div>
  );
}
