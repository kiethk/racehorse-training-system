'use client';

import type { ReactNode } from 'react';

import { Button, FilterBar, FormField, SearchInput, Select } from '@/components/ui';
import type { HorseStatus } from '@/features/stable/types';
import { cn } from '@/lib/cn';

export interface HorseFilterBarProps {
  search: string;
  onSearchChange: (value: string) => void;
  searchLabel?: string;
  searchPlaceholder?: string;
  status: HorseStatus | '';
  onStatusChange: (status: HorseStatus | '') => void;
  onApply: () => void;
  onClear: () => void;
  children?: ReactNode;
  className?: string;
}

/**
 * Controlled filter row for horse lists:
 * SearchInput + Status dropdown + optional extra controls (children) + Apply / Clear buttons.
 * Filters are applied on submit (clicking Apply or pressing Enter).
 */
export function HorseFilterBar({
  search,
  onSearchChange,
  searchLabel = 'Search horses',
  searchPlaceholder = 'Search by name, registration or breed…',
  status,
  onStatusChange,
  onApply,
  onClear,
  children,
  className = '',
}: HorseFilterBarProps) {
  return (
    <FilterBar
      className={cn('[&_input]:min-h-9 [&_select]:min-h-9', className)}
      onSubmit={(event) => {
        event.preventDefault();
        onApply();
      }}
    >
      <FormField label={searchLabel} className="min-w-[14rem] flex-[1.4]">
        <SearchInput
          value={search}
          onChange={onSearchChange}
          placeholder={searchPlaceholder}
          label={searchLabel}
        />
      </FormField>

      <FormField label="Status" className="min-w-[10rem] flex-1">
        <Select
          id="horse-status-filter"
          aria-label="Status"
          value={status}
          onChange={(event) => onStatusChange(event.target.value as HorseStatus | '')}
        >
          <option value="">All statuses</option>
          <option value="CANDIDATE">Candidate</option>
          <option value="ELIGIBLE">Eligible</option>
          <option value="REJECTED">Rejected</option>
          <option value="INACTIVE">Inactive</option>
        </Select>
      </FormField>

      {children}

      <div className="flex shrink-0 items-center gap-2">
        <Button type="submit" variant="primary">
          Apply
        </Button>
        <Button type="button" variant="secondary" onClick={onClear}>
          Clear
        </Button>
      </div>
    </FilterBar>
  );
}
