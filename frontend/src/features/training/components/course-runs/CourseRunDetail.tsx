'use client';

import { DAY_HEADING_FORMAT } from '../../lib/dateRange';
import Link from 'next/link';
import { Fragment, useCallback, useEffect, useState } from 'react';
import { BackLink, Button, ConfirmDialog, DetailSkeleton, EmptyState, HorseAvatar, PageHeader, Panel, Pill, ProgressBar, ScreenLayout, Table, TBody, Td, Th, THead, Tr } from '@/components/ui';
import { displayError, formatDate, formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { courseRunProgress } from '../../lib/courseRunProgress';
import { COURSE_RUN_STATUS, SESSION_STATUS } from '../../lib/status';
import { trainingService } from '../../services/trainingService';
import type { CourseRunDetail as CourseRunDetailData, CourseRunHorse } from '../../types';
import { CourseRunSessionHorses } from './CourseRunSessionHorses';
import { StatusPill } from '../shared/StatusPill';
import { isViewOnlyCourseRun } from '../../lib/groupLifecycle';

/** One Course Run: its progress and every Session with its time, Exercise and status. */
export function CourseRunDetail({ courseRunId }: { courseRunId: number }) {
  const [detail, setDetail] = useState<CourseRunDetailData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmCancel, setConfirmCancel] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [courseRunHorses, setCourseRunHorses] = useState<CourseRunHorse[]>([]);
  const [openSessionId, setOpenSessionId] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      const [loaded, horses] = await Promise.all([trainingService.getCourseRun(courseRunId), trainingService.getCourseRunHorses(courseRunId)]);
      setDetail(loaded);
      setCourseRunHorses(horses);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the course run.'));
    }
  }, [courseRunId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function cancelCourseRun() {
    setCancelling(true);
    try {
      setDetail(await trainingService.cancelCourseRun(courseRunId));
      toast.success('Course run cancelled.');
      setConfirmCancel(false);
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to cancel the course run.'));
    } finally {
      setCancelling(false);
    }
  }

  if (error) {
    return (
      <ScreenLayout variant="detail">
        <BackLink href="/trainer/course-runs">Back to course runs</BackLink>
        <Panel padded><EmptyState icon="alert-triangle" title="Unable to load the course run" description={error} /></Panel>
      </ScreenLayout>
    );
  }
  if (!detail) return <DetailSkeleton />;

  const { courseRun, sessions } = detail;
  const progress = courseRunProgress(courseRun.sessionsHeld, courseRun.sessionsTotal, courseRun.sessionsCancelled, courseRun.status);

  return (
    <ScreenLayout variant="detail">
      <BackLink href="/trainer/course-runs">Back to course runs</BackLink>
      <PageHeader
        title={courseRun.courseName}
        actions={
          <>
            <StatusPill status={COURSE_RUN_STATUS[courseRun.status]} size="md" />
            {!isViewOnlyCourseRun(courseRun.status, courseRun.groupDisbanded) && (courseRun.status === 'UPCOMING' || courseRun.status === 'ACTIVE') && (
              <Button variant="destructive" size="sm" onClick={() => setConfirmCancel(true)}>Cancel course run</Button>
            )}
          </>
        }
      />

      <Panel padded className="space-y-3">
        <div className="flex flex-wrap items-center gap-x-6 gap-y-1 text-sm text-[var(--color-text-secondary)]">
          <span className="flex items-center gap-1.5 font-medium text-[var(--color-text-primary)]">
            {courseRun.groupName ?? 'Removed group'}
            {courseRun.groupDisbanded && <Pill tone="neutral" size="sm">Disbanded</Pill>}
          </span>
          <span>{formatDate(courseRun.startDate)} – {formatDate(courseRun.endDate)}</span>
        </div>
        <ProgressBar label="Course run progress" value={progress.percent} />
        <p className="text-xs text-[var(--color-text-muted)]">{progress.completed}/{progress.total} sessions</p>
      </Panel>

      <Panel padded className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-sm font-semibold text-[var(--color-text-primary)]">Horses in this course run</h2>
          <span className="text-xs text-[var(--color-text-muted)]">
            {courseRunHorses.length} {courseRunHorses.length === 1 ? 'horse' : 'horses'}
          </span>
        </div>
        {courseRunHorses.length === 0 ? (
          <p className="text-sm text-[var(--color-text-muted)]">No horses.</p>
        ) : (
          <ul className="grid gap-2.5 sm:grid-cols-2 lg:grid-cols-3">
            {courseRunHorses.map((horse) => (
              <li
                key={horse.horseId}
                className="flex items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-2.5"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <HorseAvatar name={horse.horseName} size={28} rounded="md" />
                  <Link
                    href={`/trainer/horses/${horse.horseId}?tab=course-runs`}
                    className="truncate text-sm font-medium text-[var(--color-primary)] hover:underline"
                  >
                    {horse.horseName}
                  </Link>
                </div>
                <div className="shrink-0" title="Sessions attended out of sessions held or still to come">
                  <Pill tone="neutral" size="sm">
                    {horse.attended} / {horse.total} sessions
                  </Pill>
                </div>
              </li>
            ))}
          </ul>
        )}
      </Panel>

      <Panel>
        <Table bare>
          <THead>
            <Tr>
              <Th>#</Th>
              <Th>Date</Th>
              <Th>Time</Th>
              <Th>Exercise</Th>
              <Th>Horses</Th>
              <Th>Status</Th>
              <Th className="text-right">Action</Th>
            </Tr>
          </THead>
          <TBody>
            {sessions.map((session, index) => {
              const open = openSessionId === session.id;
              return (
                <Fragment key={session.id}>
                  <Tr className={open ? 'bg-[var(--color-surface-subtle)]' : undefined}>
                    <Td>{index + 1}</Td>
                    <Td>{formatDate(session.date, DAY_HEADING_FORMAT)}</Td>
                    <Td>{formatTime(session.startTime)} – {formatTime(session.endTime)}</Td>
                    <Td className="font-medium text-[var(--color-text-primary)]">{session.exerciseName}</Td>
                    <Td>
                      <span className="font-medium">{session.horseCount}</span>{' '}
                      <span className="text-xs text-[var(--color-text-muted)]">
                        {session.horseCount === 1 ? 'horse' : 'horses'}
                      </span>
                    </Td>
                    <Td><StatusPill status={SESSION_STATUS[session.status]} /></Td>
                    <Td className="text-right">
                      <Button
                        variant={open ? 'secondary' : 'tertiary'}
                        size="sm"
                        icon={open ? 'chevron-up' : 'chevron-down'}
                        aria-expanded={open}
                        aria-label={`${open ? 'Hide' : 'View'} horses of session ${index + 1}`}
                        onClick={() => setOpenSessionId(open ? null : session.id)}
                      >
                        {open ? 'Hide horses' : 'View horses'}
                      </Button>
                    </Td>
                  </Tr>
                  {open && (
                    <Tr>
                      <Td colSpan={7} className="bg-[var(--color-surface-subtle)] p-4 border-t border-[var(--color-border)]">
                        <CourseRunSessionHorses sessionId={session.id} />
                      </Td>
                    </Tr>
                  )}
                </Fragment>
              );
            })}
          </TBody>
        </Table>
      </Panel>
      <ConfirmDialog
        open={confirmCancel}
        title="Cancel course run"
        description="Every session that has not started is cancelled for the whole group, and the dates are released. Sessions already held keep their results."
        confirmLabel="Cancel course run"
        tone="danger"
        loading={cancelling}
        onConfirm={cancelCourseRun}
        onCancel={() => setConfirmCancel(false)}
      />
    </ScreenLayout>
  );
}
