'use client';

import { useMemo, useState } from 'react';
import { HorseAvatar, Panel, Pill, SearchInput, Skeleton, Tooltip } from '@/components/ui';
import type { Horse } from '@/features/stable/types';
import { cn } from '@/lib/cn';
import type { TrainingGroup } from '../types';
import { horseDragDrop, type DraggableHorseItem } from './horseDragDrop';

interface HorseLibraryProps {
  horses: Horse[];
  groups: TrainingGroup[];
  loading?: boolean;
  onAddHorse?: (group: TrainingGroup, horseId: number) => Promise<void>;
  onRemoveHorse?: (horse: DraggableHorseItem) => void;
  disabled?: boolean;
}

export function HorseLibrary({
  horses,
  groups,
  loading = false,
  onAddHorse,
  onRemoveHorse,
  disabled = false,
}: HorseLibraryProps) {
  const [search, setSearch] = useState('');

  // Drop zone to accept horses dragged back from groups to remove them
  const { dropProps, isOver } = horseDragDrop.useDropZone({
    disabled,
    accept: (horse) => Boolean(horse.sourceGroupId),
    onDrop: (horse) => {
      if (horse.sourceGroupId && onRemoveHorse) {
        onRemoveHorse(horse);
      }
    },
  });

  // Map horse IDs to their current active group if assigned
  const assignmentMap = useMemo(() => {
    const map = new Map<number, { groupId: number; groupName: string }>();
    for (const group of groups) {
      for (const member of group.members) {
        map.set(member.horseId, { groupId: group.id, groupName: group.name });
      }
    }
    return map;
  }, [groups]);

  // Split horses into ready (unassigned & eligible), blocked, and assigned
  const { ready, blocked, assigned } = useMemo(() => {
    const query = search.trim().toLowerCase();
    const filtered = query
      ? horses.filter(
          (h) =>
            h.name.toLowerCase().includes(query) ||
            h.breed?.toLowerCase().includes(query) ||
            h.stableLocation?.toLowerCase().includes(query),
        )
      : horses;

    const readyList: Horse[] = [];
    const blockedList: Horse[] = [];
    const assignedList: Horse[] = [];

    for (const horse of filtered) {
      if (assignmentMap.has(horse.id)) {
        assignedList.push(horse);
      } else if (horse.trainingDecision === 'BLOCKED' || horse.currentStatus !== 'ELIGIBLE') {
        blockedList.push(horse);
      } else {
        readyList.push(horse);
      }
    }

    return { ready: readyList, blocked: blockedList, assigned: assignedList };
  }, [horses, assignmentMap, search]);

  return (
    <div {...dropProps}>
      <Panel
        padded
        className={cn(
          'space-y-4 transition-all border',
          isOver
            ? 'ring-2 ring-[var(--color-danger)] border-[var(--color-danger)] bg-[var(--color-danger-soft)]/10'
            : 'border-[var(--color-border)]',
        )}
      >
        {/* Drop zone feedback when dragging a horse to remove */}
        {isOver && (
          <div className="flex items-center justify-center p-3 rounded-[var(--radius-md)] border-2 border-dashed border-[var(--color-danger)] bg-[var(--color-danger-soft)]/20 text-center animate-pulse">
            <p className="text-xs font-semibold text-[var(--color-danger)]">
              Release to remove horse from group
            </p>
          </div>
        )}

        {/* Header */}
        <div>
          <div className="flex items-center justify-between gap-2">
            <h3 className="text-sm font-semibold text-[var(--color-text-primary)]">Horse Library</h3>
          </div>
        </div>

        {/* Search Filter */}
        <SearchInput
          value={search}
          onChange={setSearch}
          label="Search horses"
          placeholder="Search horse name, breed, location..."
        />

        {loading ? (
          <div className="space-y-2 py-2">
            <Skeleton className="h-14 w-full rounded-md" />
            <Skeleton className="h-14 w-full rounded-md" />
            <Skeleton className="h-14 w-full rounded-md" />
          </div>
        ) : (
          <div className="scroll-slim max-h-[calc(100vh-17rem)] space-y-4 overflow-y-auto pr-1">
            {/* Section 1: Ready to Group (Top, Active & Draggable) */}
            <div className="space-y-2">
              <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-1">
                <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-primary)]">
                  Available ({ready.length})
                </span>
              </div>

              {ready.length === 0 ? (
                <p className="py-3 text-center text-xs text-[var(--color-text-muted)]">
                  {search ? 'No available horses match your search.' : 'All eligible horses are assigned to groups.'}
                </p>
              ) : (
                <div className="space-y-1.5">
                  {ready.map((horse) => (
                    <DraggableHorseCard
                      key={horse.id}
                      horse={horse}
                      disabled={disabled}
                    />
                  ))}
                </div>
              )}
            </div>

            {/* Section 2: Blocked Horses (Dimmed + Warning, Non-draggable) */}
            {blocked.length > 0 && (
              <div className="space-y-2 pt-2">
                <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-1">
                  <span className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-[var(--color-danger)]">
                    Blocked ({blocked.length})
                  </span>
                </div>

                <div className="space-y-1.5">
                  {blocked.map((horse) => (
                    <DraggableHorseCard
                      key={horse.id}
                      horse={horse}
                      disabled={disabled}
                    />
                  ))}
                </div>
              </div>
            )}

            {/* Section 3: Assigned Horses (Bottom, Dimmed) */}
            <div className="space-y-2 pt-2">
              <div className="flex items-center justify-between border-b border-[var(--color-border)] pb-1">
                <span className="text-xs font-semibold uppercase tracking-wider text-[var(--color-text-secondary)]">
                  In a group ({assigned.length})
                </span>
              </div>

              {assigned.length === 0 ? (
                <p className="py-3 text-center text-xs text-[var(--color-text-muted)]">
                  No assigned horses match your search.
                </p>
              ) : (
                <div className="space-y-1.5">
                  {assigned.map((horse) => {
                    const assignment = assignmentMap.get(horse.id);
                    return (
                      <div
                        key={horse.id}
                        className="group flex items-center justify-between gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface-subtle)] p-2 opacity-60 transition-all select-none"
                      >
                        <div className="flex min-w-0 items-center gap-2">
                          <HorseAvatar name={horse.name} size={28} rounded="md" />
                          <div className="min-w-0">
                            <p className="truncate text-xs font-semibold text-[var(--color-text-primary)]">
                              {horse.name}
                            </p>
                            <div className="flex items-center gap-1.5 text-[11px] text-[var(--color-text-muted)]">
                              {horse.breed && <span className="truncate">{horse.breed}</span>}
                              {horse.stableLocation && (
                                <span className="shrink-0">• {horse.stableLocation}</span>
                              )}
                            </div>
                          </div>
                        </div>
                        <span className="shrink-0">
                          <Pill tone="neutral" size="sm">
                            {assignment ? assignment.groupName : 'In group'}
                          </Pill>
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        )}
      </Panel>
    </div>
  );
}

function DraggableHorseCard({
  horse,
  disabled,
}: {
  horse: Horse;
  disabled: boolean;
}) {
  const isBlocked = horse.trainingDecision === 'BLOCKED' || horse.currentStatus !== 'ELIGIBLE';
  const reason =
    horse.trainingDecision === 'BLOCKED'
      ? horse.trainingDecisionReason || 'Urgent care pending'
      : horse.currentStatus !== 'ELIGIBLE'
        ? `Status: ${horse.currentStatus}`
        : '';

  const item: DraggableHorseItem = {
    id: horse.id,
    name: horse.name,
    breed: horse.breed,
    stableLocation: horse.stableLocation,
    sourceGroupId: null,
  };

  const { dragProps, isDragging } = horseDragDrop.useDraggable({
    item,
    label: horse.name,
    disabled: disabled || isBlocked,
  });

  return (
    <div
      {...(!isBlocked ? dragProps : {})}
      className={cn(
        'group flex items-center justify-between gap-2 rounded-[var(--radius-md)] border p-2 transition-all select-none',
        isBlocked
          ? 'border-[var(--color-border)] bg-[var(--color-surface-subtle)] opacity-50 cursor-not-allowed'
          : 'border-[var(--color-border)] bg-[var(--color-surface)] hover:border-[var(--color-primary-soft)] hover:shadow-xs cursor-grab active:cursor-grabbing',
        isDragging && 'opacity-50 ring-2 ring-[var(--color-primary)]',
      )}
    >
      <div className="flex min-w-0 items-center gap-2">
        <HorseAvatar name={horse.name} size={28} rounded="md" />
        <div className="min-w-0">
          <div className="flex items-center gap-1.5 flex-wrap">
            <p
              className={cn(
                'truncate text-xs font-semibold',
                isBlocked
                  ? 'text-[var(--color-text-secondary)]'
                  : 'text-[var(--color-text-primary)] group-hover:text-[var(--color-primary)]',
              )}
            >
              {horse.name}
            </p>
            {isBlocked && (
              <Tooltip content={reason}>
                <span>
                  <Pill tone="danger" size="sm" icon="alert-triangle">
                    {horse.trainingDecision === 'BLOCKED' ? 'Blocked' : horse.currentStatus}
                  </Pill>
                </span>
              </Tooltip>
            )}
          </div>
          <div className="flex items-center gap-1.5 text-[11px] text-[var(--color-text-muted)]">
            {horse.breed && <span className="truncate">{horse.breed}</span>}
            {horse.stableLocation && (
              <span className="shrink-0">• {horse.stableLocation}</span>
            )}
          </div>
        </div>
      </div>
      <div
        className={cn(
          'shrink-0 text-xs pr-1 font-mono',
          isBlocked
            ? 'text-[var(--color-text-muted)]'
            : 'text-[var(--color-text-muted)] group-hover:text-[var(--color-primary)]',
        )}
      >
        {isBlocked ? '🚫' : '⠿'}
      </div>
    </div>
  );
}
