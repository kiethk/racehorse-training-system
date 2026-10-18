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

export type StallTone = 'success' | 'danger' | 'info' | 'neutral';

/** The colour token behind each tone. The 2D dot and the 3D scene both draw from it. */
export const STALL_TONE_TOKEN: Record<StallTone, string> = {
  success: '--color-success',
  danger: '--color-danger',
  info: '--color-info',
  neutral: '--color-border-strong',
};

const STALL_TONE_DOT: Record<StallTone, string> = {
  success: 'bg-[var(--color-success)]',
  danger: 'bg-[var(--color-danger)]',
  info: 'bg-[var(--color-info)]',
  neutral: 'bg-[var(--color-border-strong)]',
};

/** Every state a stall can be drawn in, in legend order. */
export const STALL_STATES = [
  { label: 'Occupied', tone: 'success' },
  { label: 'Training blocked', tone: 'danger' },
  { label: 'Reserved', tone: 'info' },
  { label: 'Empty', tone: 'neutral' },
] as const satisfies ReadonlyArray<{ label: string; tone: StallTone }>;

export function stallToneDot(tone: StallTone) {
  return STALL_TONE_DOT[tone];
}

/** How a stall's state is drawn: one colour and one word, the same on every stable screen. */
export function stallState(stall: StableStall, horse: Horse | undefined) {
  const state = horse && horse.trainingDecision === 'BLOCKED' ? STALL_STATES[1]
    : horse || stall.status === 'OCCUPIED' ? STALL_STATES[0]
    : stall.status === 'RESERVED' ? STALL_STATES[2]
    : STALL_STATES[3];
  return { label: state.label, tone: state.tone as StallTone, dot: STALL_TONE_DOT[state.tone] };
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
      title={compact ? `Stall ${stall.stallCode}${horse ? ` · ${horse.name} (Occupied)` : ` · ${state.label}`}` : undefined}
      className={cn('flex flex-col', compact ? [
        'h-11 items-center justify-center px-1.5 py-1 text-center',
        horse && 'border-[var(--color-success)]/30 bg-[var(--color-success-soft)] font-semibold text-[#165a38]',
        !horse && stall.status === 'RESERVED' && 'border-[var(--color-info)]/30 bg-[var(--color-info-soft)] text-[var(--color-info)]',
      ] : 'min-h-24 animate-in fade-in zoom-in-95 fill-mode-backwards duration-300')}
      style={style}
    >
      <span className={cn('flex items-center gap-1', compact ? 'w-full justify-center' : 'justify-between')}>
        <span className={cn('font-metric text-xs font-bold', compact && horse ? 'text-[#165a38]' : 'text-[var(--color-text-primary)]')}>
          {stall.stallCode}
        </span>
        {!compact && <span className={cn('h-2 w-2 shrink-0 rounded-full', state.dot)} title={state.label} />}
      </span>
      {!compact && (
        <span
          className={cn(
            'mt-1.5 line-clamp-2 break-words text-xs leading-snug',
            horse ? 'font-semibold text-[var(--color-text-primary)]' : 'italic text-[var(--color-text-muted)]',
          )}
        >
          {horse ? horse.name : 'Empty'}
        </span>
      )}
      {footer && <span className="mt-auto block min-w-0 pt-2">{footer}</span>}
    </SelectableCard>
  );
}
