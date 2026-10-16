'use client';

import { Avatar, Button, Icon, IconButton } from '@/components/ui';
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

export function StaffAssignment({ target, name, personId, mixed, warning, disabled, removeReason,
  onPick, onRemove, onAssign }: Props) {
  const label = target.kind === 'trainer' ? 'Trainer' : 'Groom';
  const { dragProps } = staffDragDrop.useDraggable({
    item: { kind: target.kind, personId: personId ?? 0, source: target },
    label: name ?? label,
    disabled: disabled || personId == null,
  });
  const { dropProps, canDrop, isOver } = staffDragDrop.useDropZone({
    disabled,
    accept: staff => staff.kind === target.kind,
    onDrop: staff => onAssign(target, staff),
  });
  return (
    <div data-assignment={targetKey(target)} {...dragProps} {...dropProps}
      className={cn('flex min-w-0 items-center rounded-[var(--radius-md)] border bg-[var(--color-surface)]',
        !name && !mixed ? 'border-dashed border-[var(--color-border-strong)]' : 'border-[var(--color-border)]',
        warning && 'border-[var(--color-warning)]/40 bg-[var(--color-warning-soft)]',
        !disabled && personId != null && 'cursor-grab active:cursor-grabbing',
        canDrop && 'ring-1 ring-[var(--color-primary)]',
        isOver && 'bg-[var(--color-primary-soft)]')}
    >
      <Button variant="tertiary" disabled={disabled} onClick={() => onPick(target)}
        aria-label={`${name ? 'Change' : 'Assign'} ${label}${name ? `: ${name}` : ''}`}
        className="h-auto min-h-12 min-w-0 flex-1 justify-start gap-2 px-2.5 py-2 text-left">
        {name ? <Avatar name={name} /> : <Icon name={mixed ? 'users' : 'plus'} size={16} className="shrink-0" />}
        <span className={cn('min-w-0 truncate text-sm', !name && 'text-[var(--color-text-secondary)]')}>
          {mixed ? 'Mixed assignment' : name ?? `Assign ${label}`}
        </span>
      </Button>
      {(personId != null || mixed) && <IconButton icon={removeReason ? 'lock' : 'x'}
        label={removeReason ?? `Remove ${label}${name ? ` ${name}` : ''}`}
        disabled={disabled || !!removeReason} onClick={() => onRemove(target)} size="sm" className="mr-1 shrink-0" />}
    </div>
  );
}
