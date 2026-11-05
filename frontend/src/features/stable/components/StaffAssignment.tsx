'use client';

import { Icon, IconButton } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { AssignmentTarget, DragStaff } from '../types/manager';
import { targetKey } from '../lib/managerAssignments';
import { staffDragDrop } from './staffDragDrop';

interface Props {
  target: AssignmentTarget;
  name: string | null;
  personId: number | null;
  mixed?: boolean;
  warning?: boolean;
  disabled: boolean;
  removeReason?: string;
  onPick: (target: AssignmentTarget) => void;
  onRemove: (target: AssignmentTarget) => void;
  onAssign: (target: AssignmentTarget, staff: DragStaff) => void;
}

export function StaffAssignment({
  target,
  name,
  personId,
  mixed,
  warning,
  disabled,
  removeReason,
  onPick: _onPick,
  onRemove,
  onAssign,
}: Props) {
  const isTrainer = target.kind === 'trainer';
  const roleLabel = isTrainer ? 'Trainer' : 'Groom';

  const { dragProps } = staffDragDrop.useDraggable({
    item: { kind: target.kind, personId: personId ?? 0, source: target },
    label: name ?? roleLabel,
    disabled: disabled || personId == null,
  });

  const { dropProps, canDrop, isOver } = staffDragDrop.useDropZone({
    disabled,
    accept: (staff) => staff.kind === target.kind,
    onDrop: (staff) => onAssign(target, staff),
  });

  return (
    <div
      data-assignment={targetKey(target)}
      {...dragProps}
      {...dropProps}
      className={cn(
        'group flex min-w-0 items-center justify-between gap-2 px-1 py-1 transition-all text-xs rounded-md',
        warning && 'text-[var(--color-warning)]',
        !disabled && personId != null && 'cursor-grab active:cursor-grabbing',
        canDrop && 'ring-2 ring-[var(--color-primary)] bg-[var(--color-primary-soft)]/30',
        isOver && 'bg-[var(--color-primary-soft)] ring-2 ring-[var(--color-primary)]'
      )}
    >
      <div className="flex min-w-0 items-center gap-1.5 flex-1">
        <span className="shrink-0 text-xs font-medium text-[var(--color-text-muted)]">
          {roleLabel}:
        </span>
        <span
          className={cn(
            'truncate font-medium text-xs',
            name ? 'text-[var(--color-text-primary)]' : 'text-[var(--color-text-muted)] italic'
          )}
          title={name ?? undefined}
        >
          {mixed ? 'Mixed assignment' : name ?? 'Unassigned'}
        </span>
      </div>

      <div className="flex items-center gap-1 shrink-0">
        {removeReason ? (
          <span title={removeReason} className="text-[var(--color-text-muted)] p-0.5" role="img" aria-label={removeReason}>
            <Icon name="lock" size={13} />
          </span>
        ) : (personId != null || mixed) ? (
          <IconButton
            icon="x"
            label={`Remove ${roleLabel}${name ? ` ${name}` : ''}`}
            disabled={disabled}
            onClick={() => onRemove(target)}
            size="sm"
            className="h-5 w-5 opacity-0 group-hover:opacity-100 transition-opacity text-[var(--color-text-muted)] hover:text-[var(--color-danger)]"
          />
        ) : null}
      </div>
    </div>
  );
}
