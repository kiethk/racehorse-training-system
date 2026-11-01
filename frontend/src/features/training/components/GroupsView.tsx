'use client';

import { useCallback, useEffect, useState } from 'react';
import { Button, ConfirmDialog, EmptyState, ListSkeleton, PageHeader, Panel, ScreenLayout } from '@/components/ui';
import { displayError } from '@/lib/display';
import { toast } from '@/lib/toast';
import { disbandMessage, type DisbandImpact } from '../lib/groupLifecycle';
import { trainingService } from '../services/trainingService';
import type { TrainingGroup } from '../types';
import { AddHorseDialog } from './AddHorseDialog';
import { DisbandedGroups } from './DisbandedGroups';
import { GroupCard } from './GroupCard';
import { GroupNameDialog } from './GroupNameDialog';
import { MoveHorseDialog } from './MoveHorseDialog';

type NameDialog = { mode: 'create' } | { mode: 'rename'; group: TrainingGroup } | null;
interface HorseTarget {
  group: TrainingGroup;
  horseId: number;
  horseName: string;
}

/** The Trainer's groups: create, rename and disband a group, and add, remove or move its horses. */
export function GroupsView() {
  const [groups, setGroups] = useState<TrainingGroup[]>([]);
  const [disbanded, setDisbanded] = useState<TrainingGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [nameDialog, setNameDialog] = useState<NameDialog>(null);
  const [nameError, setNameError] = useState<string | null>(null);
  const [addTarget, setAddTarget] = useState<TrainingGroup | null>(null);
  const [moveTarget, setMoveTarget] = useState<HorseTarget | null>(null);
  const [removeTarget, setRemoveTarget] = useState<HorseTarget | null>(null);
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
      replace(await trainingService.addGroupHorse(group.id, horseId));
    } catch (cause) {
      toast.error(displayError(cause, 'Unable to add the horse.'));
    }
  }

  // A move changes two groups, so the whole list is read again.
  const moveHorse = (targetGroupId: number, horseId: number) =>
    run(async () => {
      await trainingService.moveGroupHorse(targetGroupId, horseId);
      setMoveTarget(null);
      await load();
    }, 'Unable to move the horse.');

  const removeHorse = () =>
    run(async () => {
      if (!removeTarget) return;
      replace(await trainingService.removeGroupHorse(removeTarget.group.id, removeTarget.horseId));
      setRemoveTarget(null);
    }, 'Unable to remove the horse.');

  /** Counts what disbanding will cancel before asking, so the confirmation can say it. */
  const askDisband = (group: TrainingGroup) =>
    run(async () => {
      const plans = await trainingService.getGroupPlans({ groupId: group.id });
      const unfinished = plans.filter((plan) => plan.status === 'UPCOMING' || plan.status === 'ACTIVE');
      const details = await Promise.all(unfinished.map((plan) => trainingService.getGroupPlan(plan.id)));
      const now = Date.now();
      const sessions = details
        .flatMap((detail) => detail.sessions)
        .filter((session) => session.status === 'SCHEDULED' && new Date(`${session.date}T${session.startTime}`).getTime() > now);
      setDisbandTarget({ group, impact: { plans: unfinished.length, sessions: sessions.length, hasHistory: plans.length > 0 } });
    }, 'Unable to read the group plans.');

  const disbandGroup = () =>
    run(async () => {
      if (!disbandTarget) return;
      await trainingService.disbandGroup(disbandTarget.group.id);
      setDisbandTarget(null);
      await load();
    }, 'Unable to disband the group.');

  return (
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
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
          {groups.map((group) => (
            <GroupCard
              key={group.id}
              group={group}
              onAddHorse={() => setAddTarget(group)}
              onRename={() => {
                setNameError(null);
                setNameDialog({ mode: 'rename', group });
              }}
              onDisband={() => askDisband(group)}
              onRemoveHorse={(horseId, horseName) => setRemoveTarget({ group, horseId, horseName })}
              onMoveHorse={(horseId, horseName) => setMoveTarget({ group, horseId, horseName })}
            />
          ))}
        </div>
      )}

      <DisbandedGroups groups={disbanded} />

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
      <MoveHorseDialog
        target={moveTarget}
        groups={groups}
        submitting={busy}
        onMove={moveHorse}
        onClose={() => setMoveTarget(null)}
      />
      <ConfirmDialog
        open={removeTarget !== null}
        title="Remove horse"
        description={removeTarget ? `Remove ${removeTarget.horseName} from ${removeTarget.group.name}?` : ''}
        confirmLabel="Remove"
        tone="danger"
        loading={busy}
        onConfirm={removeHorse}
        onCancel={() => setRemoveTarget(null)}
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
  );
}
