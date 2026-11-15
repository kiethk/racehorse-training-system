'use client';

import Link from 'next/link';
import { useState } from 'react';
import { Button, HorseAvatar, Panel, Pill } from '@/components/ui';
import type { TrainingGroup } from '../../types';
import { DisbandedToggle } from './DisbandedToggle';

/**
 * Disbanded groups section: shows disbanded groups in a compact card format.
 * Each card has a clean single-row toggle with an arrow that expands the horse list.
 */
export function DisbandedGroups({ groups }: { groups: TrainingGroup[] }) {
  if (groups.length === 0) return null;

  return (
    <Panel padded className="space-y-3">
      <DisbandedToggle count={groups.length}>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3 items-start">
          {groups.map((group) => (
            <DisbandedGroupCard key={group.id} group={group} />
          ))}
        </div>
      </DisbandedToggle>
    </Panel>
  );
}

function DisbandedGroupCard({ group }: { group: TrainingGroup }) {
  const [showMembers, setShowMembers] = useState(false);
  const count = group.members.length;

  return (
    <div className="flex flex-col justify-between rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-3 space-y-2">
      <div className="flex items-start justify-between gap-2 border-b border-[var(--color-border)] pb-2">
        <h4 className="truncate text-sm font-semibold text-[var(--color-text-primary)]">{group.name}</h4>
        <Pill tone="neutral" size="sm">Disbanded</Pill>
      </div>

      {count === 0 ? (
        <p className="text-xs text-[var(--color-text-muted)] py-1">No horses recorded</p>
      ) : (
        <div>
          <Button
            variant="tertiary"
            size="sm"
            className="w-full justify-start text-xs font-medium px-1 h-auto py-1"
            icon={showMembers ? 'chevron-down' : 'chevron-right'}
            onClick={() => setShowMembers((prev) => !prev)}
          >
            Horses when disbanded ({count})
          </Button>

          {showMembers && (
            <ul className="mt-2 space-y-1.5 divide-y divide-[var(--color-border)] border-t border-[var(--color-border)] pt-2">
              {group.members.map((member) => (
                <li key={member.horseId} className="flex items-center justify-between gap-2 pt-1.5 text-xs">
                  <div className="flex items-center gap-1.5 min-w-0">
                    <HorseAvatar name={member.horseName} size={20} rounded="sm" />
                    <Link
                      href={`/trainer/horses/${member.horseId}`}
                      className="truncate font-medium text-[var(--color-primary)] hover:underline"
                    >
                      {member.horseName}
                    </Link>
                  </div>
                  <span className="text-[var(--color-text-muted)] truncate shrink-0">
                    {member.groomName || 'No groom'}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  );
}
