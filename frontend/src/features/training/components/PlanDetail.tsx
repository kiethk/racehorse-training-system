'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import Link from 'next/link';
import { Button } from '@/components/ui/Button';
import { Panel } from '@/components/ui/Panel';
import { Pill } from '@/components/ui/StatusBadge';
import { EmptyState, ListSkeleton } from '@/components/ui/states';
import { ScreenLayout } from '@/components/ui/ScreenLayout';
import { PageHeader } from '@/components/ui/PageHeader';
import { displayError, formatDate } from '@/lib/display';
import { stableApi } from '@/features/stable/services/stableService';
import type { UserSummary } from '@/features/stable/types';
import { trainingApi } from '../services/trainingService';
import type { HorseTrainingPlanDetailResponse, PlanWorkoutItemResponse } from '../types';
import { CompleteWorkoutDialog } from './CompleteWorkoutDialog';

interface PlanDetailProps {
  planId: number;
}

export function PlanDetail({ planId }: PlanDetailProps) {
  const [detail, setDetail] = useState<HorseTrainingPlanDetailResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [activeWorkout, setActiveWorkout] = useState<PlanWorkoutItemResponse | null>(null);
  /** Groom name lookup — reuses the staff directory endpoint of the stable screen. */
  const [grooms, setGrooms] = useState<UserSummary[]>([]);

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [data, groomList] = await Promise.all([
        trainingApi.getPlanById(planId),
        stableApi.getGrooms(),
      ]);
      setDetail(data);
      setGrooms(groomList);
    } catch (err) {
      console.error('Unable to load training plan details:', err);
      setError(displayError(err, 'Unable to load training plan details.'));
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadData();
  }, [loadData]);

  const groomNameById = useMemo(
    () => new Map(grooms.map((g) => [g.id, g.fullName] as const)),
    [grooms],
  );

  // Progress bar formula: the denominator excludes cancelled sessions (Plan 6)
  const progress = useMemo(() => {
    if (!detail?.workouts || detail.workouts.length === 0) {
      return { percent: 0, completed: 0, total: 0, cancelled: 0 };
    }
    const completed = detail.workouts.filter((w) => w.status === 'COMPLETED').length;
    const cancelled = detail.workouts.filter((w) => w.status === 'CANCELLED').length;
    const denominator = Math.max(detail.workouts.length - cancelled, 1);
    const percent = Math.min(Math.round((completed / denominator) * 100), 100);
    return { percent, completed, total: denominator, cancelled };
  }, [detail]);

  if (loading) return <ListSkeleton rows={5} />;

  if (error || !detail) {
    return (
      <Panel padded>
        <EmptyState icon="alert-triangle" title="Training plan unavailable" description={error || 'This training plan could not be found.'} />
      </Panel>
    );
  }

  const { plan, workouts } = detail;

  return (
    <ScreenLayout variant="detail">
      <PageHeader
        title={detail.horseName ?? `Horse #${plan.horseId}`}
        description={<>Course: <strong>{detail.courseName ?? `#${plan.courseId}`}</strong><br />{formatDate(plan.startDate)} – {formatDate(plan.endDate)} · {workouts.length} planned sessions</>}
        breadcrumbs={[{ label: 'Training plans', href: '/trainer/plans' }, { label: detail.horseName ?? `Horse #${plan.horseId}` }]}
        actions={<>
          <Pill tone={plan.status === 'COMPLETED' ? 'success' : plan.status === 'ACTIVE' ? 'primary' : plan.status === 'CANCELLED' ? 'danger' : 'info'}>{plan.status}</Pill>
          <Link href="/trainer/schedule"><Button variant="secondary" size="sm">View lot schedule</Button></Link>
        </>}
      />

      {/* Progress bar */}
      <Panel padded>
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="font-medium text-[var(--color-text-primary)]">
              Progress: {progress.completed}/{progress.total} sessions ({progress.percent}%)
            </span>
            {progress.cancelled > 0 && (
              <span className="text-xs text-[var(--color-text-muted)]">
                ({progress.cancelled} cancelled)
              </span>
            )}
          </div>
          <div className="h-2.5 w-full rounded-full bg-[var(--color-surface-muted)] overflow-hidden">
            <div
              className="h-full rounded-full bg-[var(--color-primary)] transition-all duration-300"
              style={{ width: `${progress.percent}%` }}
            />
          </div>
        </div>
      </Panel>

      {/* Session list */}
      <Panel padded>
        <h2 className="text-lg font-semibold text-[var(--color-text-primary)] mb-4">
          Session schedule
        </h2>

        {workouts.length === 0 ? (
          <p className="text-xs text-[var(--color-text-muted)] italic">
            No sessions have been scheduled for this plan yet.
          </p>
        ) : (
          <div className="divide-y divide-[var(--color-border)]">
            {workouts.map((w, index) => (
              <div key={w.workoutId} className="py-3 flex flex-wrap items-center justify-between gap-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-[var(--color-primary)]">
                      #{index + 1}
                    </span>
                    <span className="text-sm font-semibold text-[var(--color-text-primary)]">
                      {w.subjectName}
                    </span>
                    <Pill
                      tone={
                        w.status === 'COMPLETED'
                          ? 'success'
                          : w.status === 'CANCELLED'
                          ? 'danger'
                          : 'info'
                      }
                      size="sm"
                    >
                      {w.status}
                    </Pill>
                  </div>
                  <div className="mt-1 text-xs text-[var(--color-text-secondary)] flex flex-wrap gap-3">
                    <span>Date: <strong>{formatDate(w.lotDate)}</strong></span>
                    <span>Time: <strong>{w.startTime} – {w.endTime}</strong></span>
                    {/*
                      lotId is SHARED with other horses, while assignedGroomId
                      belongs to THIS horse only. They are different in nature, so
                      label them in words — side by side as "#number" they would be
                      read as "the lot's groom", and one lot can have up to 6 grooms.
                    */}
                    <span>
                      🏷️ Lot #{w.lotId}
                      {w.lotOccupancy && w.lotOccupancy > 1 && (
                        <span className="text-[var(--color-text-muted)]">
                          {' '}({w.lotOccupancy - 1} other horses)
                        </span>
                      )}
                    </span>
                    {/*
                      Use ? : and NOT &&. With &&, assignedGroomId = null
                      would render nothing, making the "unassigned" state
                      invisible — when that is exactly what needs to be shown.
                    */}
                    {w.assignedGroomId ? (
                      <span>
                        Groom:{' '}
                        <strong>
                          {groomNameById.get(w.assignedGroomId) ?? `#${w.assignedGroomId}`}
                        </strong>
                      </span>
                    ) : (
                      <span className="text-[var(--color-warning)]">
                        Groom not assigned
                      </span>
                    )}
                  </div>

                  {w.status === 'COMPLETED' && (
                    <div className="mt-2 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-2 text-xs text-[var(--color-text-secondary)] space-y-0.5">
                      <div className="font-medium text-[var(--color-text-primary)]">
                        Rating: {w.performanceRating}/10
                        {w.topSpeedKmh && ` · Max speed: ${w.topSpeedKmh} km/h`}
                        {w.actualDistanceMeters && ` · Distance: ${w.actualDistanceMeters} m`}
                      </div>
                      {w.trainerFeedback && <div>Feedback: {w.trainerFeedback}</div>}
                    </div>
                  )}
                </div>

                {w.status === 'SCHEDULED' && (
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setActiveWorkout(w)}
                  >
                    Record session results
                  </Button>
                )}
              </div>
            ))}
          </div>
        )}
      </Panel>

      <CompleteWorkoutDialog
        workout={activeWorkout}
        open={activeWorkout !== null}
        onClose={() => setActiveWorkout(null)}
        onSuccess={loadData}
      />
    </ScreenLayout>
  );
}
