'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, ConfirmDialog, EmptyState, ListSkeleton, PageHeader, Panel, ScreenLayout } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { useTrainerHorses } from '../../hooks/useTrainerHorses';
import { disbandMessage, leaveMessage, type DisbandImpact } from '../../lib/groupLifecycle';
import { trainingService } from '../../services/trainingService';
import type { TrainingGroup } from '../../types';
import { AddHorseDialog } from './AddHorseDialog';
import { DisbandedGroups } from './DisbandedGroups';
import { GroupCard } from './GroupCard';
import { GroupNameDialog } from './GroupNameDialog';
import { horseDragDrop, type DraggableHorseItem } from './horseDragDrop';
import { HorseLibrary } from './HorseLibrary';

type NameDialog = { mode: 'create' } | { mode: 'rename'; group: TrainingGroup } | null;
/** A horse about to leave a group: removed (no target) or moved to another group. */
interface LeaveTarget {
  from: TrainingGroup;
  to?: TrainingGroup;
  horseId: number;
  horseName: string;
}

/** The Trainer's groups: manage active groups, roster drag & drop, and horse library sidebar. */
export function GroupsView() {
  const [groups, setGroups] = useState<TrainingGroup[]>([]);
  const [disbanded, setDisbanded] = useState<TrainingGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { horses: trainerHorses, loading: horsesLoading, reload: reloadHorses } = useTrainerHorses();
  const horseMap = useMemo(() => new Map(trainerHorses.map((h) => [h.id, h])), [trainerHorses]);

  const [nameDialog, setNameDialog] = useState<NameDialog>(null);
  const [nameError, setNameError] = useState<string | null>(null);
  const [addTarget, setAddTarget] = useState<TrainingGroup | null>(null);
  const [leaveTarget, setLeaveTarget] = useState<LeaveTarget | null>(null);
  const [disbandTarget, setDisbandTarget] = useState<{ group: TrainingGroup; impact: DisbandImpact } | null>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [live, closed] = await Promise.all([trainingService.getGroups(), trainingService.getDisbandedGroups()]);
      setGroups(live);
      setDisbanded(closed);
    } catch (cause) {
      setError(displayError(cause, 'Unable to load the groups.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load();
  }, [load]);

  /** Replaces one group in the list with the version the backend just returned. */
  function replace(updated: TrainingGroup) {
    setGroups((current) => current.map((group) => (group.id === updated.id ? updated : group)));
  }

  async function run(action: () => Promise<void>, fallback: string) {
    setBusy(true);
    try {
      await action();
    } catch (cause) {
      toast.error(displayError(cause, fallback));
    } finally {
      setBusy(false);
    }
  }

  async function saveName(name: string) {
    if (!nameDialog) return;
    setNameError(null);
    setBusy(true);
    try {
      if (nameDialog.mode === 'create') {
        const created = await trainingService.createGroup({ name });
        setGroups((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)));
      } else {
        replace(await trainingService.renameGroup(nameDialog.group.id, name));
      }
      setNameDialog(null);
    } catch (cause) {
      setNameError(displayError(cause, 'Unable to save the group.'));
    } finally {
      setBusy(false);
    }
  }

  async function addHorse(group: TrainingGroup, horseId: number) {
    try {
      const updated = await trainingService.addGroupHorse(group.id, horseId);
      replace(updated);
      toast.success('Horse added to group.');
      reloadHorses();
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to add the horse.'));
    }
  }

  /** A dragged horse leaving its group is confirmed first, since its upcoming workouts there are cancelled. */
  function askLeave(horse: DraggableHorseItem, targetGroupId?: number) {
    const from = groups.find((group) => group.id === horse.sourceGroupId);
    if (!from) return;
    const to = targetGroupId === undefined ? undefined : groups.find((group) => group.id === targetGroupId);
    setLeaveTarget({ from, to, horseId: horse.id, horseName: horse.name });
  }

  const leaveGroup = () =>
    run(async () => {
      if (!leaveTarget) return;
      if (leaveTarget.to) {
        // A move changes two groups, so the whole list is read again.
        await trainingService.moveGroupHorse(leaveTarget.to.id, leaveTarget.horseId);
        toast.success('Horse moved to new group.');
        await load();
      } else {
        replace(await trainingService.removeGroupHorse(leaveTarget.from.id, leaveTarget.horseId));
        toast.success('Horse returned to library.');
      }
      setLeaveTarget(null);
      reloadHorses();
    }, 'Unable to update the group.');

  /** Counts what disbanding will cancel before asking, so the confirmation can say it. */
  const askDisband = (group: TrainingGroup) =>
    run(async () => {
      const courseRuns = await trainingService.getCourseRuns({ groupId: group.id });
      const unfinished = courseRuns.filter((courseRun) => courseRun.status === 'UPCOMING' || courseRun.status === 'ACTIVE');
      const details = await Promise.all(unfinished.map((courseRun) => trainingService.getCourseRun(courseRun.id)));
      const now = Date.now();
      const sessions = details
        .flatMap((detail) => detail.sessions)
        .filter((session) => session.status === 'SCHEDULED' && new Date(`${session.date}T${session.startTime}`).getTime() > now);
      setDisbandTarget({ group, impact: { courseRuns: unfinished.length, sessions: sessions.length, hasHistory: courseRuns.length > 0 } });
    }, 'Unable to read the course runs.');

  const disbandGroup = () =>
    run(async () => {
      if (!disbandTarget) return;
      await trainingService.disbandGroup(disbandTarget.group.id);
      setDisbandTarget(null);
      await load();
      reloadHorses();
    }, 'Unable to disband the group.');

  return (
    <horseDragDrop.Provider>
      <ScreenLayout variant="list">
        <PageHeader
          title="Groups"
          actions={
            <Button
              variant="primary"
              icon="plus"
              onClick={() => {
                setNameError(null);
                setNameDialog({ mode: 'create' });
              }}
            >
              New group
            </Button>
          }
        />

        <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_20rem]">
          {/* Left Column: Active Groups & Disbanded */}
          <div className="space-y-6 min-w-0">
            {loading ? (
              <Panel padded><ListSkeleton rows={3} /></Panel>
            ) : error ? (
              <Panel padded>
                <EmptyState
                  icon="alert-triangle"
                  title="Unable to load the groups"
                  description={error}
                  action={<Button size="sm" icon="refresh" onClick={load}>Try again</Button>}
                />
              </Panel>
            ) : groups.length === 0 ? (
              <Panel padded>
                <EmptyState icon="users" title="No groups yet" />
              </Panel>
            ) : (
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2 items-start">
                {groups.map((group) => (
                  <GroupCard
                    key={group.id}
                    group={group}
                    horseMap={horseMap}
                    onAddHorse={addHorse}
                    onOpenAddDialog={() => setAddTarget(group)}
                    onMoveHorse={(targetGroupId, horse) => askLeave(horse, targetGroupId)}
                    onRename={() => {
                      setNameError(null);
                      setNameDialog({ mode: 'rename', group });
                    }}
                    onDisband={() => askDisband(group)}
                  />
                ))}
              </div>
            )}

            <DisbandedGroups groups={disbanded} />
          </div>

          {/* Right Column: Horse Library Sidebar */}
          <aside className="sticky top-6">
            <HorseLibrary
              horses={trainerHorses}
              groups={groups}
              loading={horsesLoading}
              onRemoveHorse={(horse) => askLeave(horse)}
              disabled={busy}
            />
          </aside>
        </div>

        <GroupNameDialog
          open={nameDialog !== null}
          title={nameDialog?.mode === 'rename' ? 'Rename group' : 'New group'}
          initialName={nameDialog?.mode === 'rename' ? nameDialog.group.name : ''}
          submitting={busy}
          error={nameError}
          onSubmit={saveName}
          onClose={() => setNameDialog(null)}
        />
        <AddHorseDialog
          group={addTarget ? (groups.find((group) => group.id === addTarget.id) ?? addTarget) : null}
          onClose={() => setAddTarget(null)}
          onAdd={addHorse}
        />
        <ConfirmDialog
          open={leaveTarget !== null}
          title={leaveTarget?.to ? 'Move horse' : 'Remove horse'}
          description={leaveTarget ? leaveMessage(leaveTarget.horseName, leaveTarget.from, leaveTarget.to) : ''}
          confirmLabel={leaveTarget?.to ? 'Move' : 'Remove'}
          tone="danger"
          loading={busy}
          onConfirm={leaveGroup}
          onCancel={() => setLeaveTarget(null)}
        />
        <ConfirmDialog
          open={disbandTarget !== null}
          title="Disband group"
          description={disbandTarget ? disbandMessage(disbandTarget.group.name, disbandTarget.impact) : ''}
          confirmLabel="Disband"
          tone="danger"
          loading={busy}
          onConfirm={disbandGroup}
          onCancel={() => setDisbandTarget(null)}
        />
      </ScreenLayout>
    </horseDragDrop.Provider>
  );
}
