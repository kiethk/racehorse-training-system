'use client';

import { useId, useRef, useState, type KeyboardEvent } from 'react';
import { cn } from '@/lib/cn';
import { controlClassName, useFieldContext } from './FormField';
import { Icon } from './Icon';

export interface CreatableComboboxOption {
  value: string;
  label: string;
  hint?: string;
}

export interface CreatableComboboxProps {
  id?: string;
  value: string;
  onChange: (value: string) => void;
  options: CreatableComboboxOption[];
  placeholder?: string;
  disabled?: boolean;
  required?: boolean;
  'aria-invalid'?: boolean;
  'aria-describedby'?: string;
  className?: string;
}

/**
 * Combobox that lets the user pick from a static suggestion list OR type a
 * free-text value. Built on a plain input + filtered dropdown, keyboard
 * accessible (ArrowUp/Down/Enter/Escape). No external dependency.
 */
export function CreatableCombobox({
  value,
  onChange,
  options,
  placeholder = 'Select or type a name',
  disabled,
  className,
  ...props
}: CreatableComboboxProps) {
  const field = useFieldContext();
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(-1);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const id = props.id ?? field?.id;
  const required = props.required ?? field?.required;
  const ariaInvalid = (props['aria-invalid'] as boolean | undefined) ?? (field?.invalid || undefined);
  const ariaDescribedBy = props['aria-describedby'] ?? field?.describedBy;

  const filtered = value.trim()
    ? options.filter((opt) => opt.label.toLowerCase().includes(value.toLowerCase()))
    : options;

  const listId = useId();

  // Closing the list also clears the highlight, so the next open starts from the top.
  const closeList = () => {
    setOpen(false);
    setHighlight(-1);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setOpen(true);
      setHighlight((prev) => Math.min(prev + 1, filtered.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlight((prev) => Math.max(prev - 1, 0));
    } else if (e.key === 'Enter' && open && highlight >= 0 && highlight < filtered.length) {
      e.preventDefault();
      onChange(filtered[highlight].value);
      closeList();
    } else if (e.key === 'Escape') {
      closeList();
    }
  };

  return (
    <span className={cn('relative block', className)}>
      <input
        ref={inputRef}
        type="text"
        id={id}
        required={required}
        aria-invalid={ariaInvalid}
        aria-describedby={ariaDescribedBy}
        aria-expanded={open}
        aria-controls={listId}
        aria-autocomplete="list"
        role="combobox"
        value={value}
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setTimeout(closeList, 150)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        disabled={disabled}
        className={cn(controlClassName, 'pr-9', disabled && 'cursor-not-allowed opacity-60')}
      />
      <Icon
        name="chevron-down"
        size={15}
        className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[var(--color-text-muted)]"
      />
      {open && filtered.length > 0 && (
        <ul
          ref={listRef}
          id={listId}
          role="listbox"
          className="absolute z-[var(--z-dropdown)] mt-1 max-h-60 w-full overflow-auto rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] py-1 shadow-[var(--shadow-popover)]"
        >
          {filtered.map((opt, i) => (
            <li
              key={opt.value}
              role="option"
              aria-selected={i === highlight}
              onMouseDown={(e) => {
                e.preventDefault();
                onChange(opt.value);
                closeList();
              }}
              className={cn(
                'cursor-pointer px-3 py-1.5 text-sm text-[var(--color-text-primary)]',
                i === highlight && 'bg-[var(--color-surface-muted)]',
              )}
            >
              <span className="font-medium">{opt.label}</span>
              {opt.hint && (
                <span className="ml-2 text-xs text-[var(--color-text-muted)]">{opt.hint}</span>
              )}
            </li>
          ))}
        </ul>
      )}
    </span>
  );
}