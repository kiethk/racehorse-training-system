'use client';

import { useCallback, useEffect, useState } from 'react';
import { BackLink, Button, ConfirmDialog, DetailSkeleton, EmptyState, PageHeader, Panel, ProgressBar, ScreenLayout, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { displayError, formatDate, formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { planProgress } from '../lib/planProgress';
import { PLAN_STATUS, SESSION_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { GroupPlanDetail as GroupPlanDetailData } from '../types';
import { StatusPill } from './StatusPill';

const DAY: Intl.DateTimeFormatOptions = { weekday: 'short', day: '2-digit', month: 'short' };

/** One Group Plan: its progress and every Session with its time, Subject and status. */
export function GroupPlanDetail({ planId }: { planId: number }) {
  const [detail, setDetail] = useState<GroupPlanDetailData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);

  const load = useCallback(async () => {
    try {
      setDetail(await trainingService.getGroupPlan(planId));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the plan.'));
    }
  }, [planId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function cancelPlan() {
    setCancelling(true);
    try {
      setDetail(await trainingService.cancelGroupPlan(planId));
      toast.success('Course cancelled.');
      setConfirmCancel(false);
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to cancel the course.'));
    } finally {
      setCancelling(false);
    }
  }

  if (error) {
    return (
      <ScreenLayout variant="detail">
        <BackLink href="/trainer/plans">Back to plans</BackLink>
        <Panel padded><EmptyState icon="alert-triangle" title="Unable to load the plan" description={error} /></Panel>
      </ScreenLayout>
    );
  }
  if (!detail) return <DetailSkeleton />;

  const { plan, sessions } = detail;
  const progress = planProgress(plan.sessionsHeld, plan.sessionsTotal, plan.sessionsCancelled, plan.status);

  return (
    <ScreenLayout variant="detail">
      <BackLink href="/trainer/plans">Back to plans</BackLink>
      <PageHeader
        title={plan.courseName}
        actions={
          <>
            <StatusPill status={PLAN_STATUS[plan.status]} size="md" />
            {(plan.status === 'UPCOMING' || plan.status === 'ACTIVE') && (
              <Button variant="destructive" size="sm" onClick={() => setConfirmCancel(true)}>Cancel course</Button>
            )}
          </>
        }
      />

      <Panel padded className="space-y-3">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-sm text-[var(--color-text-secondary)]">
          <span>{plan.groupName ?? 'Deleted group'}</span>
          <span>{formatDate(plan.startDate)} – {formatDate(plan.endDate)}</span>
        </div>
        <ProgressBar label="Plan progress" value={progress.percent} />
        <p className="text-xs text-[var(--color-text-muted)]">{progress.completed}/{progress.total} sessions</p>
      </Panel>

      <Panel>
        <Table bare>
          <THead>
            <Tr>
              <Th>#</Th>
              <Th>Date</Th>
              <Th>Time</Th>
              <Th>Subject</Th>
              <Th>Horses</Th>
              <Th>Status</Th>
            </Tr>
          </THead>
          <TBody>
            {sessions.map((session, index) => (
              <Tr key={session.id}>
                <Td>{index + 1}</Td>
                <Td>{formatDate(session.date, DAY)}</Td>
                <Td>{formatTime(session.startTime)} – {formatTime(session.endTime)}</Td>
                <Td>{session.subjectName}</Td>
                <Td>{session.horseCount}</Td>
                <Td><StatusPill status={SESSION_STATUS[session.status]} /></Td>
              </Tr>
            ))}
          </TBody>
        </Table>
      </Panel>
      <ConfirmDialog
        open={confirmCancel}
        title="Cancel course"
        description="Every session that has not started is cancelled for the whole group, and the dates are released. Sessions already held keep their results."
        confirmLabel="Cancel course"
        tone="danger"
        loading={cancelling}
        onConfirm={cancelPlan}
        onCancel={() => setConfirmCancel(false)}
      />
    </ScreenLayout>
  );
}
