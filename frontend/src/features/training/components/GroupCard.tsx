'use client';

import Link from 'next/link';
import { useMemo } from 'react';
import { Button, HorseAvatar, Icon, IconButton, Panel, Pill, Tooltip } from '@/components/ui';
import type { Horse } from '@/features/stable/types';
import { cn } from '@/lib/cn';
import { formatDate } from '@/lib/display';
import { COURSE_RUN_STATUS } from '../lib/status';
import type { GroupMember, TrainingGroup } from '../types';
import { horseDragDrop, type DraggableHorseItem } from './horseDragDrop';
import { StatusPill } from './StatusPill';

const CAPACITY = 6;

interface GroupCardProps {
  group: TrainingGroup;
  horseMap?: Map<number, Horse>;
  onAddHorse: (group: TrainingGroup, horseId: number) => void;
  onOpenAddDialog: () => void;
  onMoveHorse: (targetGroupId: number, horse: DraggableHorseItem) => void;
  onRename: () => void;
  onDisband: () => void;
}

/**
 * Group Card: Displays group header, current course, and interactive draggable horse roster.
 * - Horses inside can be dragged to other groups to move them.
 * - Horses inside can be dragged to the library to remove them.
 * - Unassigned horses from the library can be dropped here to add them.
 */
export function GroupCard({
  group,
  horseMap,
  onAddHorse,
  onOpenAddDialog,
  onMoveHorse,
  onRename,
  onDisband,
}: GroupCardProps) {
  const full = group.members.length >= CAPACITY;

  const blockedCount = useMemo(() => {
    if (!horseMap) return 0;
    return group.members.filter((m) => horseMap.get(m.horseId)?.trainingDecision === 'BLOCKED').length;
  }, [group.members, horseMap]);

  const { dropProps, isOver } = horseDragDrop.useDropZone({
    disabled: full,
    accept: (horse) =>
      !full &&
      horse.sourceGroupId !== group.id &&
      !group.members.some((m) => m.horseId === horse.id),
    onDrop: (horse) => {
      if (horse.sourceGroupId) {
        // Dragged from another group -> Move!
        onMoveHorse(group.id, horse);
      } else {
        // Dragged from Library -> Add!
        onAddHorse(group, horse.id);
      }
    },
  });

  return (
    <div {...dropProps} className="h-full">
      <Panel
        padded
        className={cn(
          'flex flex-col justify-between gap-3 h-full transition-all border',
          isOver
            ? 'ring-2 ring-[var(--color-primary)] border-[var(--color-primary)] bg-[var(--color-primary-soft)]/10'
            : 'hover:border-[var(--color-primary-soft)]',
        )}
      >
        {/* Header: Name, Count / Full, Rename (pencil), Disband (archive) */}
        <div className="flex items-start justify-between gap-2 border-b border-[var(--color-border)] pb-2.5">
          <div className="min-w-0">
            <h3 className="truncate text-base font-semibold text-[var(--color-text-primary)]">{group.name}</h3>
            <div className="mt-1 flex items-center gap-2">
              <span className="text-xs font-medium text-[var(--color-text-secondary)]">
                {group.members.length}/{CAPACITY} horses
              </span>
              {full && <Pill tone="warning" size="sm">Full</Pill>}
              {blockedCount > 0 && (
                <Pill tone="danger" size="sm" icon="alert-triangle">
                  {blockedCount} blocked
                </Pill>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1">
            <IconButton icon="pencil" label="Rename group" onClick={onRename} />
            <Tooltip content="Disband group">
              <span>
                <IconButton icon="archive" label="Disband group" onClick={onDisband} />
              </span>
            </Tooltip>
          </div>
        </div>

        {/* Current Course Box */}
        <div className="flex flex-col justify-between min-h-[76px] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-muted)] p-2.5">
          <p className="text-[11px] font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
            Current Course
          </p>
          {group.currentCourseRunStatus && group.currentCourseName ? (
            <div>
              <div className="mt-1 flex items-center justify-between gap-2">
                <div className="flex items-center gap-1.5 min-w-0">
                  <Icon name="activity" size={14} className="shrink-0 text-[var(--color-primary)]" />
                  <span className="truncate text-xs font-medium text-[var(--color-text-primary)]">
                    {group.currentCourseName}
                  </span>
                </div>
                <StatusPill status={COURSE_RUN_STATUS[group.currentCourseRunStatus]} size="sm" />
              </div>
              <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">
                {group.currentCourseRunStartDate && group.currentCourseRunEndDate
                  ? `${formatDate(group.currentCourseRunStartDate)} – ${formatDate(group.currentCourseRunEndDate)}`
                  : group.currentCourseRunEndDate
                    ? `Ends ${formatDate(group.currentCourseRunEndDate)}`
                    : group.currentCourseRunStartDate
                      ? `Starts ${formatDate(group.currentCourseRunStartDate)}`
                      : 'Ongoing'}
              </p>
            </div>
          ) : (
            <div>
              <div className="mt-1 flex items-center justify-between gap-2">
                <span className="text-xs font-medium text-[var(--color-text-secondary)]">No active course</span>
                <Pill tone="neutral" size="sm">Free</Pill>
              </div>
              <p className="mt-1 text-[11px] text-[var(--color-text-muted)]">Ready for course assignment</p>
            </div>
          )}
        </div>

        {/* Horse List: Displayed directly inside the card, all draggable */}
        <div className="space-y-2 flex-1">
          <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-1">
            <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
              Horses ({group.members.length}/{CAPACITY})
            </span>
          </div>

          {group.members.length === 0 ? (
            <div
              className={cn(
                'flex flex-col items-center justify-center p-4 rounded-[var(--radius-md)] border-2 border-dashed transition-all text-center',
                isOver
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]/20'
                  : 'border-[var(--color-border)] bg-[var(--color-surface-subtle)]',
              )}
            >
              <Icon name="plus" size={16} className="text-[var(--color-text-muted)] mb-1" />
              <p className="text-xs font-medium text-[var(--color-text-secondary)]">Drop horse here</p>
              <p className="text-[11px] text-[var(--color-text-muted)]">or click &quot;+ Add horse&quot; below</p>
            </div>
          ) : (
            <ul className="divide-y divide-[var(--color-border)] rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)]">
              {group.members.map((member) => (
                <DraggableGroupMemberRow
                  key={member.horseId}
                  member={member}
                  groupId={group.id}
                  horse={horseMap?.get(member.horseId)}
                />
              ))}
            </ul>
          )}

          {/* Empty slot drop target indicator when not full and has members */}
          {group.members.length > 0 && !full && (
            <div
              className={cn(
                'flex items-center justify-center py-1.5 px-2 rounded-[var(--radius-md)] border border-dashed transition-all text-xs',
                isOver
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary-soft)]/20 text-[var(--color-primary)] font-medium'
                  : 'border-[var(--color-border)] text-[var(--color-text-muted)]',
              )}
            >
              <span>+ Drop horse here ({CAPACITY - group.members.length} left)</span>
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="flex items-center justify-between pt-2 border-t border-[var(--color-border)]">
          <Tooltip content="The group is full" disabled={!full}>
            <span>
              <Button
                size="sm"
                variant="secondary"
                icon="plus"
                onClick={onOpenAddDialog}
                disabled={full}
              >
                Add horse
              </Button>
            </span>
          </Tooltip>
        </div>
      </Panel>
    </div>
  );
}

function DraggableGroupMemberRow({
  member,
  groupId,
  horse,
}: {
  member: GroupMember;
  groupId: number;
  horse?: Horse;
}) {
  const isBlocked = horse?.trainingDecision === 'BLOCKED' || (horse && horse.currentStatus !== 'ELIGIBLE');
  const reason =
    horse?.trainingDecision === 'BLOCKED'
      ? horse.trainingDecisionReason || 'Urgent care pending'
      : horse && horse.currentStatus !== 'ELIGIBLE'
        ? `Status: ${horse.currentStatus}`
        : '';

  const item: DraggableHorseItem = {
    id: member.horseId,
    name: member.horseName,
    sourceGroupId: groupId,
  };

  const { dragProps, isDragging } = horseDragDrop.useDraggable({
    item,
    label: member.horseName,
  });

  return (
    <li
      {...dragProps}
      className={cn(
        'group flex items-center justify-between gap-2 p-2 hover:bg-[var(--color-surface-muted)] transition-colors select-none cursor-grab active:cursor-grabbing',
        isBlocked && 'bg-[var(--color-danger-soft)]/10',
        isDragging && 'opacity-50 ring-2 ring-[var(--color-primary)] bg-[var(--color-primary-soft)]/20',
      )}
    >
      <div className="flex min-w-0 items-center gap-2">
        <HorseAvatar name={member.horseName} size={28} rounded="md" />
        <div className="min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap">
            <Link
              href={`/trainer/horses/${member.horseId}`}
              onClick={(e) => e.stopPropagation()}
              className="truncate text-xs font-semibold text-[var(--color-primary)] hover:underline block"
            >
              {member.horseName}
            </Link>
            {isBlocked && (
              <Tooltip content={reason}>
                <span>
                  <Pill tone="danger" size="sm" icon="alert-triangle">
                    {horse?.trainingDecision === 'BLOCKED' ? 'Blocked' : horse?.currentStatus}
                  </Pill>
                </span>
              </Tooltip>
            )}
          </div>
          <p className="truncate text-[11px] text-[var(--color-text-muted)]">
            Groom: {member.groomName || 'Unassigned'}
          </p>
        </div>
      </div>
      <div className="shrink-0 text-xs text-[var(--color-text-muted)] group-hover:text-[var(--color-primary)] pr-1">
        ⠿
      </div>
    </li>
  );
}
