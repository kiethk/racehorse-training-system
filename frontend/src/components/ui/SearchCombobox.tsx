'use client';

import { useId, useRef, useState, type KeyboardEvent } from 'react';
import { cn } from '@/lib/cn';
import { controlClassName } from './FormField';
import { Icon } from './Icon';

export interface SearchComboboxOption {
  value: string;
  label: string;
  hint?: string;
}

export interface SearchComboboxProps {
  /** Accessible name of the search box. */
  label: string;
  options: SearchComboboxOption[];
  /** The chosen option's value, or null while nothing is chosen. */
  value: string | null;
  /** Called only when an option is picked from the suggestions, or with null when the choice is cleared. */
  onChange: (value: string | null) => void;
  placeholder?: string;
  emptyText?: string;
  className?: string;
}

/**
 * Search box that suggests matching options as the user types. Typing never changes the value: the value changes
 * only when a suggestion is picked (click, or ArrowUp/Down then Enter), so the caller loads data once per choice.
 */
export function SearchCombobox({
  label,
  options,
  value,
  onChange,
  placeholder = 'Search',
  emptyText = 'No matches',
  className,
}: SearchComboboxProps) {
  const listId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(0);

  const selected = options.find((option) => option.value === value) ?? null;
  const needle = query.trim().toLowerCase();
  const matches = needle ? options.filter((option) => option.label.toLowerCase().includes(needle)) : options;

  function pick(option: SearchComboboxOption) {
    onChange(option.value);
    setQuery('');
    setOpen(false);
    // Blur so the next focus starts a fresh search instead of appending to the chosen label.
    inputRef.current?.blur();
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setOpen(true);
      setHighlight((index) => Math.min(index + 1, matches.length - 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setHighlight((index) => Math.max(index - 1, 0));
    } else if (event.key === 'Enter' && open && matches[highlight]) {
      event.preventDefault();
      pick(matches[highlight]);
    } else if (event.key === 'Escape') {
      setOpen(false);
    }
  }

  return (
    <span className={cn('relative block min-w-0', className)}>
      <Icon
        name="search"
        size={15}
        className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
      />
      <input
        ref={inputRef}
        type="text"
        role="combobox"
        aria-label={label}
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={open && matches[highlight] ? `${listId}-${highlight}` : undefined}
        value={open ? query : (selected?.label ?? '')}
        placeholder={selected?.label ?? placeholder}
        onChange={(event) => {
          setQuery(event.target.value);
          setHighlight(0);
          setOpen(true);
        }}
        onFocus={() => {
          setQuery('');
          setHighlight(0);
          setOpen(true);
        }}
        onBlur={() => setOpen(false)}
        onKeyDown={handleKeyDown}
        className={cn(controlClassName, 'pl-9', selected && 'pr-9')}
      />
      {selected && !open && (
        <button
          type="button"
          aria-label={`Clear ${label.toLowerCase()}`}
          onClick={() => onChange(null)}
          className="absolute right-2 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-[var(--radius-xs)] text-[var(--color-text-muted)] outline-none hover:bg-[var(--color-surface-muted)] hover:text-[var(--color-text-primary)] focus-visible:ring-2 focus-visible:ring-[var(--color-focus)]"
        >
          <Icon name="x" size={14} />
        </button>
      )}
      {open && (
        <ul
          id={listId}
          role="listbox"
          aria-label={label}
          className="absolute z-[var(--z-dropdown)] mt-1 max-h-64 w-full overflow-auto rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] py-1 shadow-[var(--shadow-popover)]"
        >
          {matches.length === 0 ? (
            <li className="px-3 py-1.5 text-sm text-[var(--color-text-muted)]">{emptyText}</li>
          ) : (
            matches.map((option, index) => (
              <li
                key={option.value}
                id={`${listId}-${index}`}
                role="option"
                aria-selected={option.value === value}
                // mousedown, not click: it runs before the input's blur closes the list.
                onMouseDown={(event) => {
                  event.preventDefault();
                  pick(option);
                }}
                onMouseEnter={() => setHighlight(index)}
                className={cn(
                  'flex cursor-pointer items-baseline justify-between gap-2 px-3 py-1.5 text-sm text-[var(--color-text-primary)]',
                  index === highlight && 'bg-[var(--color-surface-muted)]',
                  option.value === value && 'font-semibold',
                )}
              >
                <span className="truncate">{option.label}</span>
                {option.hint && <span className="shrink-0 text-xs text-[var(--color-text-muted)]">{option.hint}</span>}
              </li>
            ))
          )}
        </ul>
      )}
    </span>
  );
}
