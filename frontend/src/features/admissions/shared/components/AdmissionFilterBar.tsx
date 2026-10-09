'use client';

import type { ReactNode } from 'react';
import { Button, FilterBar, FormField, Input, SearchInput, Select } from '@/components/ui';

/**
 * The filter row above every admission list: search first, then the role's
 * extra fields (children), then Apply / Clear. Filters apply on submit.
 */
export function AdmissionFilterBar({
  search,
  onSearchChange,
  searchLabel = 'Search horse name',
  searchPlaceholder = 'Search horse name',
  onApply,
  onClear,
  children,
}: {
  search: string;
  onSearchChange: (value: string) => void;
  searchLabel?: string;
  searchPlaceholder?: string;
  onApply: () => void;
  onClear: () => void;
  children?: ReactNode;
}) {
  return (
    <FilterBar
      // Filter controls are one step more compact than form controls.
      className="[&_input]:min-h-9 [&_select]:min-h-9"
      onSubmit={(event) => {
        event.preventDefault();
        onApply();
      }}
    >
      <FormField label={searchLabel} className="min-w-[14rem] flex-[1.4]">
        <SearchInput value={search} onChange={onSearchChange} placeholder={searchPlaceholder} label={searchLabel} />
      </FormField>
      {children}
      <div className="flex shrink-0 items-center gap-2">
        <Button type="submit" variant="primary">Apply</Button>
        <Button type="button" variant="secondary" onClick={onClear}>Clear</Button>
      </div>
    </FilterBar>
  );
}

export function FilterSelect<T extends string>({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: T;
  onChange: (value: T) => void;
  options: ReadonlyArray<{ value: T; label: string }>;
}) {
  return (
    <FormField label={label} className="min-w-[10rem] flex-1">
      <Select value={value} onChange={(event) => onChange(event.target.value as T)}>
        {options.map((option) => (
          <option key={option.value || 'all'} value={option.value}>{option.label}</option>
        ))}
      </Select>
    </FormField>
  );
}

export function FilterDateRange({
  from,
  to,
  onFromChange,
  onToChange,
}: {
  from: string;
  to: string;
  onFromChange: (value: string) => void;
  onToChange: (value: string) => void;
}) {
  return (
    <>
      <FormField label="Submitted from" className="min-w-[9.5rem] flex-1">
        <Input type="date" value={from} onChange={(event) => onFromChange(event.target.value)} />
      </FormField>
      <FormField label="Submitted to" className="min-w-[9.5rem] flex-1">
        <Input type="date" min={from || undefined} value={to} onChange={(event) => onToChange(event.target.value)} />
      </FormField>
    </>
  );
}
