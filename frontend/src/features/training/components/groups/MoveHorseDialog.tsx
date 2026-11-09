'use client';

import { useState } from 'react';
import { Button, FormField, Modal, Select } from '@/components/ui';
import type { TrainingGroup } from '../../types';

interface MoveTarget {
  group: TrainingGroup;
  horseId: number;
  horseName: string;
}

/** Picks the group a horse moves to; a full group is listed but cannot be chosen. */
export function MoveHorseDialog({
  target,
  groups,
  submitting,
  onMove,
  onClose,
}: {
  target: MoveTarget | null;
  groups: TrainingGroup[];
  submitting: boolean;
  onMove: (targetGroupId: number, horseId: number) => void;
  onClose: () => void;
}) {
  return (
    <Modal
      open={target !== null}
      onClose={onClose}
      title={target ? `Move ${target.horseName}` : ''}
      size="sm"
      dismissible={!submitting}
    >
      {target && (
        <MoveForm key={target.horseId} target={target} groups={groups} submitting={submitting} onMove={onMove} onClose={onClose} />
      )}
    </Modal>
  );
}

function MoveForm({
  target,
  groups,
  submitting,
  onMove,
  onClose,
}: {
  target: MoveTarget;
  groups: TrainingGroup[];
  submitting: boolean;
  onMove: (targetGroupId: number, horseId: number) => void;
  onClose: () => void;
}) {
  const others = groups.filter((group) => group.id !== target.group.id);
  const [targetId, setTargetId] = useState<number | ''>('');

  return (
    <div className="space-y-4">
      <FormField label="Move to">
        <Select value={targetId} onChange={(event) => setTargetId(event.target.value ? Number(event.target.value) : '')}>
          <option value="">Choose a group</option>
          {others.map((group) => (
            <option key={group.id} value={group.id} disabled={group.members.length >= 6}>
              {group.name} ({group.members.length}/6)
            </option>
          ))}
        </Select>
      </FormField>
      <div className="flex justify-end gap-2">
        <Button variant="tertiary" onClick={onClose} disabled={submitting}>Cancel</Button>
        <Button
          variant="primary"
          disabled={targetId === '' || submitting}
          onClick={() => targetId !== '' && onMove(targetId, target.horseId)}
        >
          {submitting ? 'Moving...' : 'Move'}
        </Button>
      </div>
    </div>
  );
}
