'use client';

import { useId, type KeyboardEvent } from 'react';
import { cn } from '@/lib/cn';

interface ScoreScaleProps {
  /** Name of the score, e.g. "Conformation". */
  label: string;
  /** Selected score, or null while nothing is chosen. */
  value: number | null;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  required?: boolean;
  disabled?: boolean;
  className?: string;
}

/** A score picked from one horizontal bar of equal cells (0 to 10 by default); a click or the arrow keys choose a cell. */
export function ScoreScale({ label, value, onChange, min = 0, max = 10, required = false, disabled = false, className }: ScoreScaleProps) {
  const labelId = useId();
  const scores = Array.from({ length: max - min + 1 }, (_, index) => min + index);
  // The group is one tab stop: the chosen cell, or the first one while nothing is chosen.
  const tabStop = value ?? min;

  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>, score: number) {
    const step = event.key === 'ArrowRight' || event.key === 'ArrowUp' ? 1
      : event.key === 'ArrowLeft' || event.key === 'ArrowDown' ? -1 : 0;
    if (step === 0) return;
    event.preventDefault();
    const next = Math.min(max, Math.max(min, score + step));
    onChange(next);
    const radios = event.currentTarget.parentElement?.querySelectorAll<HTMLButtonElement>('[role="radio"]');
    radios?.[next - min]?.focus();
  }

  return (
    <div className={cn('min-w-0 space-y-1.5', className)}>
      <div className="flex items-baseline justify-between gap-2">
        <span id={labelId} className="block text-xs font-medium text-[var(--color-text-primary)]">
          {label}
          {required && <span className="ml-1 text-[var(--color-danger)]" aria-hidden="true">*</span>}
          {required && <span className="sr-only"> (required)</span>}
        </span>
        <span className="font-metric text-xs text-[var(--color-text-secondary)]">
          {value == null ? '—' : `${value}/${max}`}
        </span>
      </div>
      <div role="radiogroup" aria-labelledby={labelId} className="flex gap-0.5">
        {scores.map((score) => {
          const selected = score === value;
          return (
            <button
              key={score}
              type="button"
              role="radio"
              aria-checked={selected}
              tabIndex={score === tabStop ? 0 : -1}
              disabled={disabled}
              onClick={() => onChange(score)}
              onKeyDown={(event) => handleKeyDown(event, score)}
              className={cn(
                'h-9 min-w-0 flex-1 rounded-[var(--radius-xs)] border text-xs font-medium outline-none',
                'transition-colors duration-[var(--duration-fast)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]',
                'disabled:cursor-not-allowed disabled:opacity-60',
                selected
                  ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-[var(--color-text-inverse)]'
                  : 'border-[var(--color-border-strong)] bg-[var(--color-surface)] text-[var(--color-text-secondary)] hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)]',
              )}
            >
              {score}
            </button>
          );
        })}
      </div>
    </div>
  );
}
