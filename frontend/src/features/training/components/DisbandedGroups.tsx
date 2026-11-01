'use client';

import { Panel } from '@/components/ui';
import type { TrainingGroup } from '../types';
import { DisbandedToggle } from './DisbandedToggle';

/** Disbanded groups: a collapsed, read-only row, shown only when there is at least one. */
export function DisbandedGroups({ groups }: { groups: TrainingGroup[] }) {
  if (groups.length === 0) return null;

  return (
    <Panel padded>
      <DisbandedToggle count={groups.length}>
        <ul className="grid grid-cols-1 gap-2 md:grid-cols-2 xl:grid-cols-3">
          {groups.map((group) => (
            <li
              key={group.id}
              className="truncate rounded-lg border border-[var(--color-border)] bg-[var(--color-surface-muted)] px-3 py-2 text-sm text-[var(--color-text-muted)]"
            >
              {group.name}
            </li>
          ))}
        </ul>
      </DisbandedToggle>
    </Panel>
  );
}
