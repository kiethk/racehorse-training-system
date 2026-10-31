'use client';

import { Button, Icon, IconButton, LinkButton, Panel, Tooltip } from '@/components/ui';
import { formatDate } from '@/lib/display';
import { PLAN_STATUS } from '../lib/status';
import type { TrainingGroup } from '../types';
import { StatusPill } from './StatusPill';

const CAPACITY = 6;

/** One group: its horses with their Groom in a tooltip, the course it runs, and the actions on it. */
export function GroupCard({
  group,
  onAddHorse,
  onRename,
  onDelete,
  onRemoveHorse,
  onMoveHorse,
}: {
  group: TrainingGroup;
  onAddHorse: () => void;
  onRename: () => void;
  onDelete: () => void;
  onRemoveHorse: (horseId: number, horseName: string) => void;
  onMoveHorse: (horseId: number, horseName: string) => void;
}) {
  const full = group.members.length >= CAPACITY;

  return (
    <Panel padded className="flex h-full flex-col gap-4">
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0">
          <h3 className="truncate text-base font-semibold text-[var(--color-text-primary)]">{group.name}</h3>
          <p className="text-xs text-[var(--color-text-secondary)]">{group.members.length}/{CAPACITY} horses</p>
        </div>
        <div className="flex items-center gap-1">
          <IconButton icon="file-text" label="Rename group" onClick={onRename} />
          <IconButton icon="x" label="Delete group" onClick={onDelete} />
        </div>
      </div>

      {group.members.length === 0 ? (
        <p className="text-sm text-[var(--color-text-muted)]">No horses yet</p>
      ) : (
        <ul className="flex flex-wrap gap-2">
          {group.members.map((member) => (
            <li
              key={member.horseId}
              className="flex items-center gap-1 rounded-full border border-[var(--color-border)] bg-[var(--color-surface-muted)] py-1 pl-3 pr-1 text-sm"
            >
              <Tooltip content={member.groomName ? `Groom: ${member.groomName}` : 'No groom'}>
                <span className="cursor-help">{member.horseName}</span>
              </Tooltip>
              <IconButton icon="rotate" label={`Move ${member.horseName}`} onClick={() => onMoveHorse(member.horseId, member.horseName)} />
              <IconButton icon="x" label={`Remove ${member.horseName}`} onClick={() => onRemoveHorse(member.horseId, member.horseName)} />
            </li>
          ))}
        </ul>
      )}

      <div className="mt-auto flex items-center justify-between gap-2 border-t border-[var(--color-border)] pt-3">
        {group.currentPlanStatus && group.currentCourseName ? (
          <Tooltip content={group.currentPlanEndDate ? `Ends ${formatDate(group.currentPlanEndDate)}` : ''}>
            <span className="flex min-w-0 cursor-help items-center gap-2">
              <Icon name="activity" size={14} className="shrink-0 text-[var(--color-text-muted)]" />
              <span className="truncate text-sm text-[var(--color-text-primary)]">{group.currentCourseName}</span>
              <StatusPill status={PLAN_STATUS[group.currentPlanStatus]} />
            </span>
          </Tooltip>
        ) : (
          <span className="text-sm text-[var(--color-text-muted)]">Free</span>
        )}
        <div className="flex items-center gap-2">
          {group.members.length > 0 && (
            <LinkButton size="sm" href={`/trainer/plans/new?groupId=${group.id}`}>Assign course</LinkButton>
          )}
          <Tooltip content="The group is full" disabled={!full}>
            <span>
              <Button size="sm" icon="plus" onClick={onAddHorse} disabled={full}>Add horse</Button>
            </span>
          </Tooltip>
        </div>
      </div>
    </Panel>
  );
}
