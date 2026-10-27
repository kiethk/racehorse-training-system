'use client';

import type { ReactNode } from 'react';

import { Button, FilterBar, FormField, SearchInput, Select } from '@/components/ui';
import type { HorseStatus } from '@/features/stable/types';
import { cn } from '@/lib/cn';

export interface StatusOption<S extends string> {
  value: S;
  label: string;
}

const HORSE_STATUS_OPTIONS: StatusOption<HorseStatus>[] = [
  { value: 'CANDIDATE', label: 'Candidate' },
  { value: 'ELIGIBLE', label: 'Eligible' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'INACTIVE', label: 'Inactive' },
];

export interface HorseFilterBarProps<S extends string = HorseStatus> {
  search: string;
  onSearchChange: (value: string) => void;
  searchLabel?: string;
  searchPlaceholder?: string;
  status: S | '';
  onStatusChange: (status: S | '') => void;
  /** Statuses in the dropdown; defaults to the horse statuses (another list of horses passes its own, e.g. plan status). */
  statusOptions?: StatusOption<S>[];
  onApply: () => void;
  onClear: () => void;
  children?: ReactNode;
  className?: string;
}

/**
 * Controlled filter row for horse lists:
 * SearchInput + Status dropdown (horse statuses unless `statusOptions` is given) + optional extra controls (children) + Apply / Clear buttons.
 * Filters are applied on submit (clicking Apply or pressing Enter).
 */
export function HorseFilterBar<S extends string = HorseStatus>({
  search,
  onSearchChange,
  searchLabel = 'Search horses',
  searchPlaceholder = 'Search by name, registration or breed…',
  status,
  onStatusChange,
  statusOptions = HORSE_STATUS_OPTIONS as StatusOption<S>[],
  onApply,
  onClear,
  children,
  className = '',
}: HorseFilterBarProps<S>) {
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
          onChange={(event) => onStatusChange(event.target.value as S | '')}
        >
          <option value="">All statuses</option>
          {statusOptions.map((option) => (
            <option key={option.value} value={option.value}>{option.label}</option>
          ))}
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
