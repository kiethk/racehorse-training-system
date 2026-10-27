'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  BackLink,
  Button,
  ConfirmDialog,
  EmptyState,
  LinkButton,
  ListSkeleton,
  PageHeader,
  PageSection,
  Panel,
  Pill,
  ProgressBar,
  ScreenLayout,
} from '@/components/ui';
import { displayError, formatDate, formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { cancelledLabel, planProgress } from '../lib/planProgress';
import { surfaceLabel } from '../lib/subjectOptions';
import { PLAN_STATUS, WORKOUT_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type {
  HorseTrainingPlanDetailResponse,
  PlanWorkoutItemResponse,
  WorkoutResultPreviewResponse,
} from '../types';
import { CompleteWorkoutDialog } from './CompleteWorkoutDialog';
import { RatingPill } from './RatingPill';
import { StatusPill } from './StatusPill';

function WorkoutRow({
  workout,
  index,
  onRecord,
  onEdit,
  preparingResult,
}: {
  workout: PlanWorkoutItemResponse;
  index: number;
  onRecord: () => Promise<void>;
  onEdit: () => void;
  preparingResult: boolean;
}) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-3 py-3">
      <div className="min-w-0 flex-1 space-y-1">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs font-semibold text-[var(--color-primary)]">#{index + 1}</span>
          <span className="text-sm font-semibold text-[var(--color-text-primary)]">{workout.subjectName}</span>
          <StatusPill status={WORKOUT_STATUS[workout.status]} />
        </div>

        <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-[var(--color-text-secondary)]">
          <span>Date: <strong>{formatDate(workout.lotDate)}</strong></span>
          <span>Time: <strong>{formatTime(workout.startTime)} – {formatTime(workout.endTime)}</strong></span>
          {workout.surfaceType && <span>Surface: <strong>{surfaceLabel(workout.surfaceType)}</strong></span>}
          {workout.lotOccupancy != null && workout.lotOccupancy > 1 && (
            <span>With {workout.lotOccupancy - 1} other {workout.lotOccupancy === 2 ? 'horse' : 'horses'}</span>
          )}
        </div>

        {workout.status === 'COMPLETED' && (
          <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] px-3 py-2 text-xs text-[var(--color-text-secondary)]">
            <RatingPill rating={workout.performanceRating} />
            {workout.topSpeedKmh != null && <span>Max speed: {workout.topSpeedKmh} km/h</span>}
            {workout.actualDistanceMeters != null && <span>Distance: {workout.actualDistanceMeters} m</span>}
            {workout.trainerFeedback && <span className="basis-full">Feedback: {workout.trainerFeedback}</span>}
          </div>
        )}
      </div>

      <div className="flex items-center gap-2">
        {workout.status === 'SCHEDULED' && (
          <Button
            variant="primary"
            size="sm"
            onClick={() => void onRecord()}
            loading={preparingResult}
          >
            Record Workout Result
          </Button>
        )}

        {workout.status === 'COMPLETED' && (
          <Button
            variant="secondary"
            size="sm"
            onClick={onEdit}
          >
            Edit workout result
          </Button>
        )}
      </div>
    </li>
  );
}

export function PlanDetail({ planId }: { planId: number }) {
  const [detail, setDetail] = useState<HorseTrainingPlanDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeWorkout, setActiveWorkout] = useState<PlanWorkoutItemResponse | null>(null);
  const [resultPreview, setResultPreview] = useState<WorkoutResultPreviewResponse | null>(null);
  const [preparingWorkoutId, setPreparingWorkoutId] = useState<number | null>(null);
  const [isEditMode, setIsEditMode] = useState(false);
  const [confirmingCancel, setConfirmingCancel] = useState(false);
  const [cancellingPlan, setCancellingPlan] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setDetail(await trainingService.getPlanById(planId));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load training plan details.'));
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  const progress = useMemo(() => {
    const workouts = detail?.workouts ?? [];
    return planProgress(
      workouts.filter((w) => w.status === 'COMPLETED').length,
      workouts.length,
      workouts.filter((w) => w.status === 'CANCELLED').length,
      detail?.plan.status ?? 'NONE',
    );
  }, [detail]);

  async function openResultForm(workout: PlanWorkoutItemResponse, isEdit = false) {
    setIsEditMode(isEdit);
    if (isEdit) {
      setResultPreview({
        actualDistanceMeters: workout.actualDistanceMeters ?? 1000,
        actualDurationMinutes: workout.actualDurationMinutes ?? 60,
        topSpeedKmh: workout.topSpeedKmh ?? 50,
        averageSpeedKmh: workout.averageSpeedKmh ?? 40,
        averageHeartRate: workout.averageHeartRate ?? 140,
        maxHeartRate: workout.maxHeartRate ?? 180,
        recoveryHeartRate: workout.recoveryHeartRate ?? 90,
      });
      setActiveWorkout(workout);
      return;
    }

    setPreparingWorkoutId(workout.workoutId);
    try {
      const preview = await trainingService.getWorkoutResultPreview(workout.workoutId);
      setResultPreview(preview);
      setActiveWorkout(workout);
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to prepare workout results.'));
    } finally {
      setPreparingWorkoutId(null);
    }
  }

  function closeResultForm() {
    setActiveWorkout(null);
    setResultPreview(null);
    setIsEditMode(false);
  }

  if (loading && !detail) return <ListSkeleton rows={5} />;

  if (error || !detail) {
    return (
      <Panel padded>
        <EmptyState
          icon="alert-triangle"
          title="Plan not found"
          description={error ?? 'This plan does not exist.'}
          action={<LinkButton href="/trainer/plans" variant="secondary" size="sm">Back to plans</LinkButton>}
        />
      </Panel>
    );
  }

  const { plan, workouts } = detail;
  const canCancel = plan.status === 'UPCOMING' || plan.status === 'ACTIVE';
  // Same rule as the backend: only sessions whose lot has not ended are cancelled; a past one stays to be recorded.
  const pendingSessions = workouts.filter(
    (w) => w.status === 'SCHEDULED' && new Date(`${w.lotDate}T${w.endTime}`) > new Date(),
  ).length;

  async function confirmCancelPlan() {
    setCancellingPlan(true);
    try {
      setDetail(await trainingService.cancelPlan(plan.id));
      toast.success('Plan cancelled.');
      setConfirmingCancel(false);
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to cancel the plan.'));
    } finally {
      setCancellingPlan(false);
    }
  }
  const horseName = detail.horseName ?? `Horse #${plan.horseId}`;

  return (
    <ScreenLayout variant="detail">
      <BackLink href="/trainer/plans">Back to plans</BackLink>
      <PageHeader
        title={horseName}
        description={
          <>
            Course: <strong>{detail.courseName ?? `#${plan.courseId}`}</strong>
            <br />
            {formatDate(plan.startDate)} – {formatDate(plan.endDate)} · {workouts.length} scheduled sessions
          </>
        }
        breadcrumbs={[{ label: 'Training plans', href: '/trainer/plans' }, { label: horseName }]}
        actions={
          <div className="flex items-center gap-2">
            <Pill tone={PLAN_STATUS[plan.status].tone}>{PLAN_STATUS[plan.status].label}</Pill>
            {canCancel && (
              <Button variant="destructive" size="sm" onClick={() => setConfirmingCancel(true)}>
                Cancel plan
              </Button>
            )}
          </div>
        }
      />

      <Panel padded className="space-y-2">
        {progress.allCancelled ? (
          <p className="text-sm font-medium text-[var(--color-text-primary)]">
            Progress: all {progress.cancelled} sessions cancelled
          </p>
        ) : (
          <>
            <div className="flex items-center justify-between text-sm">
              <span className="font-medium text-[var(--color-text-primary)]">
                Progress: {progress.completed}/{progress.total} sessions ({progress.percent}%)
              </span>
              {progress.cancelled > 0 && (
                <span className="text-xs text-[var(--color-text-muted)]">{cancelledLabel(progress.cancelled)}</span>
              )}
            </div>
            <ProgressBar label="Plan progress" value={progress.percent} />
          </>
        )}
      </Panel>

      <PageSection title="Session schedule">
        <Panel padded>
          {workouts.length === 0 ? (
            <p className="text-xs text-[var(--color-text-muted)]">No sessions have been scheduled for this plan yet.</p>
          ) : (
            <ul className="divide-y divide-[var(--color-border)]">
              {workouts.map((workout, index) => (
                <WorkoutRow
                  key={workout.workoutId}
                  workout={workout}
                  index={index}
                  onRecord={() => openResultForm(workout, false)}
                  onEdit={() => openResultForm(workout, true)}
                  preparingResult={preparingWorkoutId === workout.workoutId}
                />
              ))}
            </ul>
          )}
        </Panel>
      </PageSection>

      <ConfirmDialog
        open={confirmingCancel}
        title={`Cancel this plan for ${horseName}?`}
        description={
          <>
            The {pendingSessions} sessions that have not taken place yet will be cancelled and their time slots released.
            Recorded results are kept. This cannot be undone.
          </>
        }
        tone="danger"
        confirmLabel="Cancel plan"
        cancelLabel="Keep plan"
        loading={cancellingPlan}
        onConfirm={confirmCancelPlan}
        onCancel={() => setConfirmingCancel(false)}
      />

      <CompleteWorkoutDialog
        key={`${activeWorkout?.workoutId ?? 'none'}-${isEditMode ? 'edit' : 'new'}`}
        workout={activeWorkout}
        preview={resultPreview}
        isEdit={isEditMode}
        open={activeWorkout !== null && resultPreview !== null}
        onClose={closeResultForm}
        onSuccess={load}
      />
    </ScreenLayout>
  );
}
