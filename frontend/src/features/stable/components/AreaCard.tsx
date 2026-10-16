'use client';

import type { ReactNode } from 'react';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger, IconButton, Panel, Pill } from '@/components/ui';
import { cn } from '@/lib/cn';
import { StallCell } from '../shared/components/StallCell';
import type { StableStall } from '../types';
import type { AssignmentDraft, AssignmentTarget, DragStaff, StableSnapshot } from '../types/manager';
import { areaView, horseMap, personName } from '../lib/managerAssignments';
import { StaffAssignment } from './StaffAssignment';
import { staffDragDrop } from './staffDragDrop';

interface Props {
  view: ReturnType<typeof areaView>;
  data: StableSnapshot;
  draft: AssignmentDraft;
  horses: ReturnType<typeof horseMap>;
  disabled: boolean;
  onPick: (target: AssignmentTarget) => void;
  onRemove: (target: AssignmentTarget) => void;
  onAssign: (target: AssignmentTarget, staff: DragStaff) => void;
  onSelectStall: (stall: StableStall) => void;
  onDelete: () => void;
}

export function AreaCard({ view, data, draft, horses, onSelectStall, onDelete, ...assignment }: Props) {
  const { area, occupied, stalls, blocks, missingTrainer, hasGrooms, attention } = view;
  const trainerId = draft.trainers[area.id];
  const { dropProps } = staffDragDrop.useDropZone({
    disabled: assignment.disabled || area.type !== 'REGULAR',
    accept: staff => staff.kind === 'trainer',
    onDrop: staff => assignment.onAssign({ kind: 'trainer', areaId: area.id }, staff),
  });
  return (
    <Panel data-area-card={area.code} tone={attention ? 'warning' : 'default'} className="min-w-0 overflow-hidden"
      {...dropProps}>
      <div className="flex items-center justify-between gap-3 px-4 py-4">
        <div>
          <h2 className="text-lg font-semibold">Area {area.code}</h2>
          <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">{occupied} / {stalls.length} occupied</p>
        </div>
        <div className="flex items-center gap-1.5">
          {attention > 0 && <Pill tone="warning" size="sm">Needs assignment</Pill>}
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <IconButton icon="more" label={`Area ${area.code} options`} size="sm" disabled={assignment.disabled} />
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem tone="danger" onSelect={onDelete}>Delete area</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>
      <div className="space-y-3 px-3 pb-3">
        {area.type === 'REGULAR' && <StaffAssignment {...assignment} target={{ kind: 'trainer', areaId: area.id }}
          personId={trainerId} name={trainerId == null ? null : personName(data, 'trainer', trainerId)}
          warning={missingTrainer} removeReason={hasGrooms ? 'Remove Groom assignments before removing Trainer' : undefined} />}
        <div className="grid grid-cols-2 gap-3">
          {blocks.map(block => (
            <GroomBlock key={block.number} block={block} areaId={area.id} disabled={assignment.disabled} onAssign={assignment.onAssign}>
              <div className="grid grid-cols-3 gap-1.5">
                {block.stalls.map(stall => <StallCell key={stall.id} stall={stall} horse={horses.get(stall.id)}
                  selected={false} onSelect={onSelectStall} compact />)}
              </div>
              <StaffAssignment {...assignment} disabled={assignment.disabled || !block.complete}
                target={{ kind: 'groom', areaId: area.id, blockNumber: block.number }}
                personId={block.groomId} name={block.groomId == null ? null : personName(data, 'groom', block.groomId)}
                mixed={block.mixed} warning={block.missingGroom}
                removeReason={block.occupied > 0 ? 'Block has horses — swap Groom instead' : undefined} />
              {!block.complete && <p className="text-xs text-[var(--color-danger)]">Incomplete block</p>}
            </GroomBlock>
          ))}
        </div>
      </div>
    </Panel>
  );
}

function GroomBlock({ block, areaId, disabled, onAssign, children }: {
  block: ReturnType<typeof areaView>['blocks'][number];
  areaId: number;
  disabled: boolean;
  onAssign: Props['onAssign'];
  children: ReactNode;
}) {
  const { dropProps, isOver } = staffDragDrop.useDropZone({
    disabled: disabled || !block.complete,
    accept: staff => staff.kind === 'groom',
    onDrop: staff => onAssign({ kind: 'groom', areaId, blockNumber: block.number }, staff),
  });
  return <div {...dropProps} className={cn('min-w-0 space-y-2 rounded-[var(--radius-md)] border p-2',
    block.missingGroom ? 'border-[var(--color-warning)]/40 bg-[var(--color-warning-soft)]/40'
      : 'border-[var(--color-border)] bg-[var(--color-surface-subtle)]',
    isOver && 'ring-1 ring-[var(--color-primary)]')}>{children}</div>;
}
