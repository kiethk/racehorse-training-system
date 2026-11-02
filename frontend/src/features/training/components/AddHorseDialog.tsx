'use client';

import { useEffect, useState } from 'react';
import { Button, EmptyState, HorseAvatar, Modal, Notice, Skeleton, Tooltip } from '@/components/ui';
import { cn } from '@/lib/cn';
import { displayError } from '@/lib/display';
import { trainingService } from '../services/trainingService';
import type { GroupCandidate, TrainingGroup } from '../types';

/**
 * Lists the Trainer's horses for one group. A horse that cannot join is dimmed and says why in a tooltip, so the Trainer never
 * has to guess which rule (same groom, full group, already grouped, resting) stands in the way.
 */
export function AddHorseDialog({
  group,
  onClose,
  onAdd,
}: {
  group: TrainingGroup | null;
  onClose: () => void;
  onAdd: (group: TrainingGroup, horseId: number) => Promise<void>;
}) {
  return (
    <Modal open={group !== null} onClose={onClose} title={group ? `Add a horse to ${group.name}` : ''} size="md">
      {group && <CandidateList key={group.id} group={group} onAdd={onAdd} />}
    </Modal>
  );
}

function CandidateList({
  group,
  onAdd,
}: {
  group: TrainingGroup;
  onAdd: (group: TrainingGroup, horseId: number) => Promise<void>;
}) {
  const [candidates, setCandidates] = useState<GroupCandidate[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [adding, setAdding] = useState<number | null>(null);

  useEffect(() => {
    let current = true;
    trainingService
      .getGroupCandidates(group.id)
      .then((list) => current && setCandidates(list))
      .catch((cause) => current && setError(displayError(cause, 'Unable to load the horses.')));
    return () => {
      current = false;
    };
  }, [group.id, group.members.length]);

  async function add(horseId: number) {
    setAdding(horseId);
    try {
      await onAdd(group, horseId);
    } finally {
      setAdding(null);
    }
  }

  if (error) return <Notice tone="error">{error}</Notice>;
  if (!candidates) return <Skeleton className="h-40 w-full" />;
  if (candidates.length === 0) {
    return <EmptyState icon="horse" title="No horses available" />;
  }

  return (
    <ul className="max-h-[60vh] space-y-2 overflow-y-auto">
      {candidates.map((candidate) => (
        <li
          key={candidate.horseId}
          className={cn(
            'flex items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-border)] p-3',
            !candidate.joinable && 'opacity-60',
          )}
        >
          <div className="flex min-w-0 items-center gap-3">
            <HorseAvatar name={candidate.horseName} size={32} />
            <div className="min-w-0">
              <p className="truncate text-sm font-medium text-[var(--color-text-primary)]">{candidate.horseName}</p>
              {candidate.groomName && (
                <p className="truncate text-xs text-[var(--color-text-secondary)]">{candidate.groomName}</p>
              )}
            </div>
          </div>
          <Tooltip content={candidate.reason ?? ''} disabled={candidate.joinable}>
            <span className={cn(!candidate.joinable && 'cursor-help')}>
              <Button
                size="sm"
                variant="primary"
                disabled={!candidate.joinable || adding !== null}
                onClick={() => add(candidate.horseId)}
              >
                {adding === candidate.horseId ? 'Adding...' : 'Add'}
              </Button>
            </span>
          </Tooltip>
        </li>
      ))}
    </ul>
  );
}
