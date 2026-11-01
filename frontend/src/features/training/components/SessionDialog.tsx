'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, ConfirmDialog, IconButton, Modal, Notice, Pill, Skeleton, Tooltip } from '@/components/ui';
import { displayError, formatDate, formatTime } from '@/lib/display';
import { toast } from '@/lib/toast';
import { sessionHasStarted } from '../lib/groupSchedule';
import { SESSION_STATUS } from '../lib/status';
import { trainingService } from '../services/trainingService';
import type { ScheduleSession, SessionDetail, SessionHorse } from '../types';
import { SessionRescheduleDialog } from './SessionRescheduleDialog';
import { SessionResultDialog } from './SessionResultDialog';
import { StatusPill } from './StatusPill';
import { groupLabel } from '../lib/disbandMessage';

type Confirm = { kind: 'cancelSession' } | { kind: 'cancelWorkout' | 'didNotRun'; horse: SessionHorse } | null;

const CONFIRM_TEXT = {
  cancelSession: { title: 'Cancel session', confirm: 'Cancel session', text: 'Cancel this session for the whole group? It is not made up.' },
  cancelWorkout: { title: 'Cancel for one horse', confirm: 'Cancel', text: 'Take this horse out of this session? It stays in the group.' },
  didNotRun: { title: 'Did not run', confirm: 'Mark', text: 'Mark this horse as not running? It will not count towards its attendance.' },
} as const;

/**
 * One Session with its horses. Cancelling or moving the Session and cancelling one horse are offered only before the Session has
 * started; once it is Held, each horse can have its result recorded, or be marked as not running.
 */
export function SessionDialog({
  sessionId,
  canManage,
  onClose,
  onChanged,
}: {
  sessionId: number | null;
  /** Trainers may change the Session; Grooms and Owners only look. */
  canManage: boolean;
  onClose: () => void;
  onChanged: () => Promise<void> | void;
}) {
  return (
    <Modal open={sessionId !== null} onClose={onClose} title="Session" size="lg">
      {sessionId !== null && (
        <SessionBody key={sessionId} sessionId={sessionId} canManage={canManage} onClose={onClose} onChanged={onChanged} />
      )}
    </Modal>
  );
}

function SessionBody({
  sessionId,
  canManage,
  onClose,
  onChanged,
}: {
  sessionId: number;
  canManage: boolean;
  onClose: () => void;
  onChanged: () => Promise<void> | void;
}) {
  const [detail, setDetail] = useState<SessionDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirm, setConfirm] = useState<Confirm>(null);
  const [recording, setRecording] = useState<SessionHorse | null>(null);
  const [moving, setMoving] = useState<ScheduleSession | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setDetail(await trainingService.getSession(sessionId));
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the session.'));
    }
  }, [sessionId]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  async function changed() {
    await load();
    await onChanged();
  }

  async function runConfirmed() {
    if (!confirm || !detail) return;
    setBusy(true);
    try {
      if (confirm.kind === 'cancelSession') {
        await trainingService.cancelSession(detail.session.id);
        toast.success('Session cancelled.');
      } else if (confirm.kind === 'cancelWorkout') {
        await trainingService.cancelSessionWorkout(confirm.horse.workoutId);
        toast.success('Cancelled for this horse.');
      } else {
        await trainingService.markDidNotRun(confirm.horse.workoutId);
        toast.success('Marked as not running.');
      }
      setConfirm(null);
      await changed();
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to complete this action.'));
    } finally {
      setBusy(false);
    }
  }

  if (error) return <Notice tone="error">{error}</Notice>;
  if (!detail) return <Skeleton className="h-48 w-full" />;

  const { session, horses } = detail;
  const started = sessionHasStarted(session);
  const canChange = canManage && session.status === 'SCHEDULED' && !started && !session.groupDisbanded;
  const held = session.status === 'HELD';

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 className="text-base font-semibold text-[var(--color-text-primary)]">
            {groupLabel(session.groupName, session.groupDisbanded)} · {session.subjectName}
          </h3>
          <p className="text-sm text-[var(--color-text-secondary)]">
            {formatDate(session.date, { weekday: 'long', day: '2-digit', month: 'short' })} · {formatTime(session.startTime)}–{formatTime(session.endTime)}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <StatusPill status={SESSION_STATUS[session.status]} size="md" />
          {canChange && (
            <>
              <Button size="sm" onClick={() => setMoving(session)}>Move</Button>
              <Button size="sm" variant="destructive" onClick={() => setConfirm({ kind: 'cancelSession' })}>Cancel session</Button>
            </>
          )}
        </div>
      </div>

      <ul className="divide-y divide-[var(--color-border)] rounded-[var(--radius-md)] border border-[var(--color-border)]">
        {horses.map((horse) => (
          <li key={horse.workoutId} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">{horse.horseName}</p>
              {horse.groomName && <p className="truncate text-xs text-[var(--color-text-secondary)]">{horse.groomName}</p>}
            </div>
            <div className="flex items-center gap-2">
              {horse.status === 'CANCELLED' && <Pill tone="danger" size="sm">Cancelled</Pill>}
              {horse.status === 'SCHEDULED' && <Pill tone="info" size="sm">Scheduled</Pill>}
              {horse.status === 'HELD' && horse.hasResult && (
                <Pill tone="success" size="sm">Rating {horse.performanceRating}</Pill>
              )}
              {horse.status === 'HELD' && !horse.hasResult && <Pill tone="warning" size="sm">Needs result</Pill>}

              {canManage && horse.status === 'SCHEDULED' && canChange && (
                <Tooltip content="Cancel for this horse">
                  <span>
                    <IconButton icon="x" label={`Cancel ${horse.horseName}`} onClick={() => setConfirm({ kind: 'cancelWorkout', horse })} />
                  </span>
                </Tooltip>
              )}
              {canManage && horse.status === 'HELD' && horse.hasResult && (
                <Button size="sm" onClick={() => setRecording(horse)}>Edit result</Button>
              )}
              {canManage && held && horse.status === 'HELD' && !horse.hasResult && (
                <>
                  <Button size="sm" variant="primary" onClick={() => setRecording(horse)}>Record result</Button>
                  <Button size="sm" onClick={() => setConfirm({ kind: 'didNotRun', horse })}>Did not run</Button>
                </>
              )}
            </div>
          </li>
        ))}
      </ul>

      <div className="flex justify-end">
        <Button variant="tertiary" onClick={onClose}>Close</Button>
      </div>

      <SessionResultDialog horse={recording} onClose={() => setRecording(null)} onRecorded={changed} />
      <SessionRescheduleDialog session={moving} onClose={() => setMoving(null)} onMoved={changed} />
      <ConfirmDialog
        open={confirm !== null}
        title={confirm ? CONFIRM_TEXT[confirm.kind].title : ''}
        description={confirm ? CONFIRM_TEXT[confirm.kind].text : ''}
        confirmLabel={confirm ? CONFIRM_TEXT[confirm.kind].confirm : 'Confirm'}
        tone="danger"
        loading={busy}
        onConfirm={runConfirmed}
        onCancel={() => setConfirm(null)}
      />
    </div>
  );
}
