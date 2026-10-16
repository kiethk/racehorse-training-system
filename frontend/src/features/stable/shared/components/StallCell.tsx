import type { CSSProperties, ReactNode } from 'react';
import { SelectableCard } from '@/components/ui';
import { cn } from '@/lib/cn';
import type { Horse, StableStall } from '../../types';

interface StallCellProps {
  stall: StableStall;
  horse: Horse | undefined;
  selected: boolean;
  /** Outside the viewer's own scope: still visible, but faded. */
  dimmed?: boolean;
  /** One line under the horse name, supplied by the screen's lens (task progress, groom name…). */
  footer?: ReactNode;
  onSelect: (stall: StableStall) => void;
  style?: CSSProperties;
  /** Smaller presentation for assignment boards; the default floor plan stays unchanged. */
  compact?: boolean;
}

/** How a stall's state is drawn: one dot colour and one word, the same on every stable screen. */
export function stallState(stall: StableStall, horse: Horse | undefined) {
  if (stall.status === 'MAINTENANCE') {
    return { label: 'Maintenance', dot: 'bg-[var(--color-warning)]' };
  }
  if (horse && horse.trainingDecision === 'BLOCKED') {
    return { label: 'Training blocked', dot: 'bg-[var(--color-danger)]' };
  }
  if (horse || stall.status === 'OCCUPIED') {
    return { label: 'Occupied', dot: 'bg-[var(--color-success)]' };
  }
  if (stall.status === 'RESERVED') {
    return { label: 'Reserved', dot: 'bg-[var(--color-info)]' };
  }
  return { label: 'Empty', dot: 'bg-[var(--color-border-strong)]' };
}

/** One stall on the floor plan. Role-neutral: what a role adds goes through `footer`. */
export function StallCell({ stall, horse, selected, dimmed = false, footer, onSelect, style, compact = false }: StallCellProps) {
  const state = stallState(stall, horse);

  return (
    <SelectableCard
      selected={selected}
      dimmed={dimmed}
      empty={!horse}
      onClick={() => onSelect(stall)}
      aria-label={`Stall ${stall.stallCode}, ${horse ? horse.name : 'no horse'}, ${state.label}`}
      title={compact ? `${stall.stallCode} · ${horse?.name ?? state.label}` : undefined}
      className={cn('flex flex-col', compact ? [
        'min-h-16 items-center justify-center gap-1 px-1.5 py-2 text-center',
        horse && 'border-[var(--color-success)]/20 bg-[var(--color-success-soft)]',
        !horse && stall.status === 'RESERVED' && 'border-[var(--color-info)]/20 bg-[var(--color-info-soft)]',
      ] : 'min-h-24 animate-in fade-in zoom-in-95 fill-mode-backwards duration-300')}
      style={style}
    >
      <span className="flex items-center justify-between gap-1">
        <span className="font-metric text-xs font-bold text-[var(--color-text-primary)]">{stall.stallCode}</span>
        {!compact && <span className={cn('h-2 w-2 shrink-0 rounded-full', state.dot)} title={state.label} />}
      </span>
      <span
        className={cn(
          'break-words text-xs leading-snug',
          compact ? 'line-clamp-1 w-full' : 'mt-1.5 line-clamp-2',
          horse ? 'font-semibold text-[var(--color-text-primary)]' : 'italic text-[var(--color-text-muted)]',
        )}
      >
        {horse ? horse.name : compact ? state.label : 'Empty'}
      </span>
      {footer && <span className="mt-auto block min-w-0 pt-2">{footer}</span>}
    </SelectableCard>
  );
}
