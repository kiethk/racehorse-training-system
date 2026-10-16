'use client';

import { Panel, Pill } from '@/components/ui';
import { cn } from '@/lib/cn';
import { StallCell } from '../shared/components/StallCell';
import type { StableStall } from '../types';
import type { AssignmentDraft, AssignmentTarget, DragStaff, StableSnapshot } from '../types/manager';
import { areaView, horseMap, personName } from '../lib/managerAssignments';
import { StaffAssignment } from './StaffAssignment';

interface Props {
  view: ReturnType<typeof areaView>;
  data: StableSnapshot;
  draft: AssignmentDraft;
  horses: ReturnType<typeof horseMap>;
  disabled: boolean;
  dragging: DragStaff | null;
  onPick: (target: AssignmentTarget) => void;
  onRemove: (target: AssignmentTarget) => void;
  onDrag: (staff: DragStaff | null) => void;
  onAssign: (target: AssignmentTarget, staff: DragStaff) => void;
  onSelectStall: (stall: StableStall) => void;
}

export function AreaCard({ view, data, draft, horses, onSelectStall, ...assignment }: Props) {
  const { area, occupied, stalls, blocks, missingTrainer, hasGrooms, attention } = view;
  const trainerId = draft.trainers[area.id];
  return (
    <Panel data-area-card={area.code} tone={attention ? 'warning' : 'default'} className="min-w-0 overflow-hidden"
      onDragOver={e => { if (!assignment.disabled && assignment.dragging?.kind === 'trainer' && area.type === 'REGULAR') e.preventDefault(); }}
      onDrop={e => {
        if (!assignment.disabled && assignment.dragging?.kind === 'trainer' && area.type === 'REGULAR') {
          e.preventDefault(); assignment.onAssign({ kind: 'trainer', areaId: area.id }, assignment.dragging); assignment.onDrag(null);
        }
      }}>
      <div className="flex items-center justify-between gap-3 px-4 py-4">
        <div>
          <h2 className="text-lg font-semibold">Area {area.code}</h2>
          <p className="mt-0.5 text-xs text-[var(--color-text-secondary)]">{occupied} / {stalls.length} occupied</p>
        </div>
        {attention > 0 && <Pill tone="warning" size="sm">Needs assignment</Pill>}
      </div>
      <div className="space-y-3 px-3 pb-3">
        {area.type === 'REGULAR' && <StaffAssignment {...assignment} target={{ kind: 'trainer', areaId: area.id }}
          personId={trainerId} name={trainerId == null ? null : personName(data, 'trainer', trainerId)}
          warning={missingTrainer} removeReason={hasGrooms ? 'Remove Groom assignments before removing Trainer' : undefined} />}
        <div className="grid grid-cols-2 gap-3">
          {blocks.map(block => (
            <div key={block.number}
              onDragOver={e => { if (!assignment.disabled && assignment.dragging?.kind === 'groom' && block.complete) e.preventDefault(); }}
              onDrop={e => {
                if (!assignment.disabled && assignment.dragging?.kind === 'groom' && block.complete) {
                  e.preventDefault(); e.stopPropagation();
                  assignment.onAssign({ kind: 'groom', areaId: area.id, blockNumber: block.number }, assignment.dragging); assignment.onDrag(null);
                }
              }} className={cn('min-w-0 space-y-2 rounded-[var(--radius-md)] border p-2',
              block.missingGroom ? 'border-[var(--color-warning)]/40 bg-[var(--color-warning-soft)]/40'
                : 'border-[var(--color-border)] bg-[var(--color-surface-subtle)]')}>
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
            </div>
          ))}
        </div>
      </div>
    </Panel>
  );
}
