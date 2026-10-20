'use client';

import { cn } from '@/lib/cn';
import { Icon, type IconName } from './Icon';

export interface SegmentOption<T extends string> {
  value: T;
  label: string;
  icon?: IconName;
  count?: number;
}

interface SegmentProps<T extends string> {
  options: SegmentOption<T>[];
  value: T;
  onChange: (value: T) => void;
  /** Accessible name for the group, e.g. "Status filter". */
  label: string;
  className?: string;
}

const focusRing = 'outline-none focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]';

/** Mutually exclusive view switch on a subtle track (e.g. Week / Month, Grid / List). */
export function SegmentedControl<T extends string>({ options, value, onChange, label, className }: SegmentProps<T>) {
  return (
    <div
      role="radiogroup"
      aria-label={label}
      className={cn('inline-flex gap-0.5 rounded-[var(--radius-md)] bg-[var(--color-surface-muted)] p-0.5', className)}
    >
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'inline-flex h-8 items-center gap-1.5 rounded-[var(--radius-sm)] px-3 text-xs font-medium transition-colors duration-[var(--duration-fast)]',
              focusRing,
              active
                ? 'bg-[var(--color-surface)] text-[var(--color-text-primary)] shadow-[var(--shadow-panel)]'
                : 'text-[var(--color-text-secondary)] hover:text-[var(--color-text-primary)]',
            )}
          >
            {option.icon && <Icon name={option.icon} size={14} />}
            {option.label}
          </button>
        );
      })}
    </div>
  );
}

/** Pill-shaped quick filters above a list (e.g. All / Active / Completed), with optional counts. */
export function FilterChips<T extends string>({ options, value, onChange, label, className }: SegmentProps<T>) {
  return (
    <div role="radiogroup" aria-label={label} className={cn('flex flex-wrap gap-1.5', className)}>
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(option.value)}
            className={cn(
              'inline-flex h-8 items-center gap-1.5 rounded-full border px-3 text-xs font-medium transition-colors duration-[var(--duration-fast)]',
              focusRing,
              active
                ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-text-inverse)]'
                : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]',
            )}
          >
            {option.icon && <Icon name={option.icon} size={13} />}
            {option.label}
            {typeof option.count === 'number' && (
              <span className={cn('font-metric text-[10px]', active ? 'opacity-80' : 'text-[var(--color-text-muted)]')}>
                {option.count}
              </span>
            )}
          </button>
        );
      })}
    </div>
  );
}
